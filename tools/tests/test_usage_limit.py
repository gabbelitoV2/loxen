import sys
import unittest
from datetime import datetime, timezone
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import usage_limit

FIXTURES = Path(__file__).resolve().parent / "fixtures"
NOW = datetime(2026, 9, 25, 10, 0, tzinfo=timezone.utc)


def failed_run(text, error="rate_limit"):
    return [
        {"type": "assistant", "error": error,
         "message": {"role": "assistant", "content": [{"type": "text", "text": text}]}},
        {"type": "result", "subtype": "success", "is_error": True, "result": text},
    ]


def stockholm_available():
    return usage_limit.zone_named("Europe/Stockholm") is not timezone.utc


class DetectSuite(unittest.TestCase):
    def test_execution_file_with_rejected_rate_limit_event(self):
        result = usage_limit.detect(usage_limit.load_messages(FIXTURES / "execution-limit.json"), True, NOW)
        self.assertTrue(result["limited"])
        self.assertEqual(result["resets_at"], datetime(2026, 9, 25, 15, 0, tzinfo=timezone.utc))
        self.assertIn("rate_limit_event rejected (five_hour)", result["evidence"])
        self.assertIn("assistant error rate_limit", result["evidence"])

    def test_successful_run_is_not_limited(self):
        messages = usage_limit.load_messages(FIXTURES / "execution-success.json")
        self.assertFalse(usage_limit.detect(messages, False, NOW)["limited"])
        self.assertIn("Re-ported", usage_limit.summary(messages))

    def test_limit_text_in_a_tool_result_is_ignored(self):
        messages = usage_limit.load_messages(FIXTURES / "execution-success.json")
        messages[-1] = {"type": "result", "subtype": "error_max_turns", "is_error": True, "errors": ["max turns"]}
        self.assertFalse(usage_limit.detect(messages, True, NOW)["limited"])

    def test_rejected_while_drawing_on_extra_usage_is_not_a_stop(self):
        messages = [{"type": "rate_limit_event", "rate_limit_info": {"status": "rejected", "overageStatus": "allowed",
                                                                     "isUsingOverage": True, "resetsAt": 1790348400}},
                    {"type": "result", "subtype": "error_max_turns", "is_error": True, "errors": ["max turns"]}]
        self.assertFalse(usage_limit.detect(messages, True, NOW)["limited"])

    def test_missing_execution_file(self):
        self.assertEqual(usage_limit.load_messages(FIXTURES / "does-not-exist.json"), [])
        self.assertFalse(usage_limit.detect([], True, NOW)["limited"])

    def test_session_limit_text_with_time_zone(self):
        result = usage_limit.detect(failed_run("You've hit your session limit · resets 3pm (UTC)"), True, NOW)
        self.assertTrue(result["limited"])
        self.assertEqual(result["resets_at"], datetime(2026, 9, 25, 15, 0, tzinfo=timezone.utc))

    @unittest.skipUnless(stockholm_available(), "no time zone database")
    def test_reset_in_stockholm_time(self):
        result = usage_limit.detect(failed_run("You've hit your limit · resets 5pm (Europe/Stockholm)"), True, NOW)
        self.assertEqual(result["resets_at"], datetime(2026, 9, 25, 15, 0, tzinfo=timezone.utc))

    def test_five_hour_limit_resets_tomorrow_when_the_hour_passed(self):
        result = usage_limit.detect(failed_run("5-hour limit reached ∙ resets 9:30am"), True, NOW)
        self.assertEqual(result["resets_at"], datetime(2026, 9, 26, 9, 30, tzinfo=timezone.utc))

    def test_weekly_limit_with_date(self):
        result = usage_limit.detect(failed_run("Weekly limit reached ∙ resets Oct 6, 1pm"), True, NOW)
        self.assertEqual(result["resets_at"], datetime(2026, 10, 6, 13, 0, tzinfo=timezone.utc))

    def test_legacy_epoch_format(self):
        result = usage_limit.detect(failed_run("Claude AI usage limit reached|1790348400", error="unknown"), True, NOW)
        self.assertTrue(result["limited"])
        self.assertEqual(result["resets_at"], datetime(2026, 9, 25, 15, 0, tzinfo=timezone.utc))

    def test_relative_reset(self):
        result = usage_limit.detect(failed_run("You've reached your Opus limit, resets in 2h 30m"), True, NOW)
        self.assertEqual(result["resets_at"], datetime(2026, 9, 25, 12, 30, tzinfo=timezone.utc))

    def test_limit_without_reset_time(self):
        result = usage_limit.detect(failed_run("You're out of extra usage"), True, NOW)
        self.assertTrue(result["limited"])
        self.assertIsNone(result["resets_at"])

    def test_other_failures_are_not_limits(self):
        for text in ("API Error: 500 Internal server error", "Invalid API key · Please run /login",
                     "Credit balance is too low"):
            self.assertFalse(usage_limit.detect(failed_run(text, error="unknown"), True, NOW)["limited"], text)


if __name__ == "__main__":
    unittest.main()
