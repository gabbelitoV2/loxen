import re
import subprocess
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import repair

WORKFLOWS = TOOLS.parent / ".github" / "workflows"

try:
    import yaml
except ImportError:
    yaml = None


def load(name):
    document = yaml.safe_load((WORKFLOWS / name).read_text(encoding="utf-8"))
    document["on"] = document.pop(True, document.get("on"))
    return document


def steps(job):
    return {step.get("name") or step.get("uses"): step for step in job["steps"]}


@unittest.skipIf(yaml is None, "PyYAML is not installed")
class WorkflowSuite(unittest.TestCase):
    def setUp(self):
        self.sync = load("android.yml")
        self.repair = load("repair.yml")

    def test_sync_workflow_manages_the_issue(self):
        self.assertEqual(self.sync["permissions"], {"contents": "write", "issues": "write"})
        self.assertEqual(set(self.sync["on"]), {"schedule", "workflow_dispatch", "push"})
        job = self.sync["jobs"]["build"]
        step = steps(job)["Sync with eerimoq/moblin"]
        self.assertIn("python tools/sync.py --commit --push --issue", step["run"])
        self.assertEqual(step["env"]["GH_TOKEN"], "${{ github.token }}")
        self.assertEqual(step["id"], "sync")
        self.assertLess(step["timeout-minutes"], job["timeout-minutes"])

    def test_a_sync_that_stops_without_a_report_still_opens_the_issue(self):
        job_steps = list(self.sync["jobs"]["build"]["steps"])
        names = [step.get("name") for step in job_steps]
        step = job_steps[names.index("Open the needs-repair issue when the sync stopped without a report")]
        self.assertEqual(names.index("Sync with eerimoq/moblin") + 1, names.index(step["name"]))
        self.assertIn("always()", step["if"])
        self.assertIn("steps.sync.outputs.published != 'true'", step["if"])
        self.assertIn("python tools/sync_report.py --sync-failed", step["run"])
        self.assertEqual(step["env"]["GH_TOKEN"], "${{ github.token }}")

    def test_repair_triggers(self):
        triggers = self.repair["on"]
        self.assertEqual(triggers["workflow_run"]["workflows"], [self.sync["name"]])
        self.assertEqual(triggers["workflow_run"]["types"], ["completed"])
        self.assertEqual(triggers["schedule"], [{"cron": "17 0,4,12,16,20 * * *"}])
        inputs = triggers["workflow_dispatch"]["inputs"]
        self.assertEqual(inputs["mode"]["options"], ["repair", "verify-only"])
        self.assertEqual(inputs["force"]["type"], "boolean")

    def test_repair_shares_the_concurrency_group_with_the_sync(self):
        job = self.repair["jobs"]["repair"]
        self.assertEqual(job["concurrency"]["group"], "sync-and-build")
        self.assertEqual(self.sync["concurrency"]["group"],
                         "${{ github.event_name == 'push' && 'push-build' || 'sync-and-build' }}")
        self.assertFalse(job["concurrency"]["cancel-in-progress"])
        self.assertFalse(self.sync["concurrency"]["cancel-in-progress"])
        self.assertNotIn("concurrency", self.repair)
        self.assertNotIn("concurrency", self.repair["jobs"]["gate"])

    def test_repair_never_starts_from_a_pull_request_run(self):
        self.assertEqual(self.repair["jobs"]["gate"]["if"],
                         "github.event_name != 'workflow_run' || github.event.workflow_run.event != 'pull_request'")
        self.assertNotIn("pull_request", self.repair["on"])
        self.assertNotIn("pull_request_target", self.repair["on"])
        checkout = self.repair["jobs"]["repair"]["steps"][0]
        self.assertEqual(checkout["with"]["ref"], "main")

    def test_permissions(self):
        self.assertEqual(self.repair["permissions"], {"contents": "read"})
        self.assertEqual(self.repair["jobs"]["gate"]["permissions"],
                         {"contents": "read", "issues": "read", "actions": "read"})
        self.assertEqual(self.repair["jobs"]["repair"]["permissions"], {"contents": "write", "issues": "write"})
        self.assertEqual(self.repair["jobs"]["repair"]["if"], "needs.gate.outputs.run == 'true'")

    def test_claude_step(self):
        step = steps(self.repair["jobs"]["repair"])["Repair with Claude"]
        self.assertEqual(step["uses"], "anthropics/claude-code-action@v1")
        self.assertTrue(step["continue-on-error"])
        options = step["with"]
        self.assertEqual(options["claude_code_oauth_token"], "${{ secrets.CLAUDE_CODE_OAUTH_TOKEN }}")
        self.assertEqual(options["github_token"], "${{ github.token }}")
        self.assertEqual(options["prompt"], "${{ steps.start.outputs.prompt }}")
        self.assertEqual(options["settings"], '{"autoContinueAtUsageLimit": false}')
        self.assertNotIn("anthropic_api_key", options)
        arguments = options["claude_args"]
        self.assertRegex(arguments, r"--max-turns \d+")
        disallowed = re.search(r'--disallowedTools "([^"]+)"', arguments).group(1).split(",")
        for tool in ("Bash(git push:*)", "Bash(git commit:*)", "Bash(gh:*)"):
            self.assertIn(tool, disallowed)
        allowed = re.search(r'--allowedTools "([^"]+)"', arguments).group(1).split(",")
        self.assertIn("Bash(./gradlew:*)", allowed)
        self.assertFalse([tool for tool in allowed if tool in ("Bash", "Bash(*)", "Bash(git:*)")])

    def test_steps_after_claude_depend_on_the_limit_check(self):
        job_steps = steps(self.repair["jobs"]["repair"])
        self.assertEqual(job_steps["Stop when Claude hit its usage limit"]["if"],
                         "steps.claude.outcome == 'success' || steps.claude.outcome == 'failure'")
        for name in ("Put back files the repair must not change", "Verify, commit and push"):
            self.assertEqual(job_steps[name]["if"], "steps.limit.outputs.limited == 'false'")

    def test_guarded_files_match_the_verifier(self):
        run = steps(self.repair["jobs"]["repair"])["Put back files the repair must not change"]["run"]
        guarded = re.search(r"guarded=\(([^)]*)\)", run).group(1).split()
        self.assertEqual(sorted(guarded), sorted(path.rstrip("/") for path in repair.GUARDED))

    def test_repair_commands_exist(self):
        text = (WORKFLOWS / "repair.yml").read_text(encoding="utf-8")
        used = set(re.findall(r"python3? tools/repair\.py (\w+)", text))
        self.assertEqual(used, {"upstream", "verify", "start", "limit", "finish"})
        self.assertIn("arguments=(gate ", text)
        joined = re.sub(r"\\\n\s*", " ", text)
        for command in used | {"gate"}:
            result = subprocess.run([sys.executable, str(TOOLS / "repair.py"), command, "--help"],
                                    capture_output=True, text=True, encoding="utf-8")
            self.assertEqual(result.returncode, 0, result.stderr)
            for line in re.findall(rf"tools/repair\.py {command}\b([^\n]*)", joined):
                for flag in re.findall(r"--[\w-]+", line):
                    self.assertIn(flag, result.stdout, f"repair.py {command} does not take {flag}")
        self.assertIn("python tools/repair.py start --force", text)
        self.assertIn('--attempt "$ATTEMPT"', joined.split("tools/repair.py limit")[1].splitlines()[0])

    def test_sync_report_command_exists(self):
        result = subprocess.run([sys.executable, str(TOOLS / "sync_report.py"), "--help"], capture_output=True,
                                text=True, encoding="utf-8")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("--sync-failed", result.stdout)


@unittest.skipIf(yaml is None, "PyYAML is not installed")
class PlayWorkflowSuite(unittest.TestCase):
    def setUp(self):
        self.sync = load("android.yml")
        self.play = load("play.yml")
        self.release = self.play["jobs"]["release"]

    def test_runs_after_a_successful_sync_and_by_hand(self):
        triggers = self.play["on"]
        self.assertEqual(set(triggers), {"workflow_run", "workflow_dispatch"})
        self.assertEqual(triggers["workflow_run"]["workflows"], [self.sync["name"]])
        self.assertEqual(triggers["workflow_run"]["types"], ["completed"])
        condition = self.play["jobs"]["gate"]["if"]
        self.assertIn("github.event.workflow_run.conclusion == 'success'", condition)
        self.assertIn("github.event.workflow_run.event == 'schedule'", condition)
        self.assertNotIn("'push'", condition)
        self.assertEqual(self.release["needs"], "gate")
        self.assertEqual(self.release["if"], "needs.gate.outputs.signing == 'true'")

    def test_has_its_own_concurrency_group_and_reads_only(self):
        self.assertEqual(self.play["permissions"], {"contents": "read"})
        self.assertEqual(self.play["concurrency"]["group"], "play-internal")
        self.assertFalse(self.play["concurrency"]["cancel-in-progress"])
        self.assertNotIn("sync-and-build", self.play["concurrency"]["group"])

    def test_builds_main_with_the_full_history(self):
        checkout = self.release["steps"][0]
        self.assertTrue(checkout["uses"].startswith("actions/checkout@"))
        self.assertEqual(checkout["with"]["ref"], "main")
        self.assertEqual(checkout["with"]["fetch-depth"], 0)

    def test_secrets_only_reach_the_steps_that_use_them(self):
        self.assertNotIn("secrets.", str(self.release.get("env", {})))
        job_steps = steps(self.release)
        build = job_steps["Build the signed app bundle and APK"]
        self.assertEqual(sorted(build["env"]), ["MOBLIN_UPLOAD_KEYSTORE_BASE64", "MOBLIN_UPLOAD_KEYSTORE_PASSWORD",
                                                "MOBLIN_UPLOAD_KEY_ALIAS", "MOBLIN_UPLOAD_KEY_PASSWORD"])
        for name, value in build["env"].items():
            self.assertEqual(value, f"${{{{ secrets.{name} }}}}")
        with_secrets = [name for name, step in job_steps.items() if "secrets." in str(step)]
        self.assertEqual(sorted(with_secrets), sorted([
            "Build the signed app bundle and APK", "Decide whether main changed since the last upload",
            "Upload to the internal testing track",
        ]))
        gate = self.play["jobs"]["gate"]["steps"][0]
        self.assertNotIn("secrets.", gate["run"])
        self.assertEqual(list(gate["env"]), ["SIGNING"])

    def test_uploads_only_what_the_decision_allows(self):
        job_steps = steps(self.release)
        self.assertEqual(job_steps["Build the signed app bundle and APK"]["if"], "steps.decide.outputs.build == 'true'")
        upload = job_steps["Upload to the internal testing track"]
        self.assertEqual(upload["if"], "steps.decide.outputs.upload == 'true'")
        self.assertRegex(upload["uses"], r"^r0adkll/upload-google-play@[0-9a-f]{40}$")
        self.assertEqual(upload["with"]["tracks"], "internal")
        self.assertEqual(upload["with"]["releaseFiles"], "app/build/outputs/bundle/release/app-release.aab")
        self.assertEqual(upload["with"]["serviceAccountJsonPlainText"], "${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}")
        self.assertEqual(self.release["env"]["PACKAGE_NAME"], "com.loxen.app")
        artifact = next(step for step in self.release["steps"] if step.get("uses", "").startswith("actions/upload-artifact@"))
        release = "loxen-${{ steps.version.outputs.code }}-${{ steps.version.outputs.name }}"
        self.assertEqual(artifact["with"]["name"], release)
        self.assertEqual(job_steps["Name the release files"]["env"]["RELEASE"], release)
        self.assertEqual(job_steps["Name the release files"]["if"], "steps.decide.outputs.build == 'true'")
        cleanup = job_steps["Remove the decoded upload key"]
        self.assertEqual(cleanup["if"], "always()")
        self.assertIn("app/build/upload-key", cleanup["run"])

    def test_play_command_exists(self):
        run = steps(self.release)["Decide whether main changed since the last upload"]["run"]
        self.assertIn("python tools/play.py decide", run)
        result = subprocess.run([sys.executable, str(TOOLS / "play.py"), "decide", "--help"], capture_output=True,
                                text=True, encoding="utf-8")
        self.assertEqual(result.returncode, 0, result.stderr)
        for flag in re.findall(r"--[\w-]+", run.split("tools/play.py decide")[1].splitlines()[0]):
            self.assertIn(flag, result.stdout)

    def test_actions_run_on_node_24(self):
        for name in ("android.yml", "repair.yml", "play.yml"):
            text = (WORKFLOWS / name).read_text(encoding="utf-8")
            for action, version in re.findall(r"uses: (actions/checkout|actions/setup-java|actions/setup-python|"
                                              r"gradle/actions/setup-gradle|actions/upload-artifact)@v(\d+)", text):
                minimum = {"actions/checkout": 5, "actions/setup-java": 5, "actions/setup-python": 6,
                           "gradle/actions/setup-gradle": 5, "actions/upload-artifact": 6}[action]
                self.assertGreaterEqual(int(version), minimum, f"{name}: {action}@v{version}")


if __name__ == "__main__":
    unittest.main()
