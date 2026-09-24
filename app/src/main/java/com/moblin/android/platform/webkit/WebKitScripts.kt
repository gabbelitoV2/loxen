package com.moblin.android.platform.webkit

import org.json.JSONArray
import org.json.JSONObject

internal const val webKitBridgeName = "moblinBridge"

internal object WebKitScripts {
    fun bridgeInstaller(handlerNames: List<String>): String {
        val bridge = JSONObject.quote(webKitBridgeName)
        return """
(function() {
  if (typeof Uint8Array.fromBase64 !== 'function') {
    Object.defineProperty(Uint8Array, 'fromBase64', {
      configurable: true,
      writable: true,
      value: function(text) {
        var binary = atob(String(text));
        var bytes = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) {
          bytes[i] = binary.charCodeAt(i);
        }
        return bytes;
      }
    });
  }
  var names = ${JSONArray(handlerNames)};
  if (names.length === 0) {
    return;
  }
  window.webkit = window.webkit || {};
  window.webkit.messageHandlers = window.webkit.messageHandlers || {};
  names.forEach(function(name) {
    window.webkit.messageHandlers[name] = {
      postMessage: function(message) {
        window[$bridge].post(name, JSON.stringify(message === undefined ? null : message));
      }
    };
  });
})();
"""
    }

    fun documentStart(script: WKUserScript): String {
        return mainFrameOnly(script.source, script.forMainFrameOnly)
    }

    fun documentEndAtDocumentStart(script: WKUserScript): String {
        val wrapped = """
(function() {
  var moblinRunUserScript = function() {
${script.source}
  };
  if (document.readyState === 'loading') {
    window.addEventListener('DOMContentLoaded', moblinRunUserScript, { once: true });
  } else {
    moblinRunUserScript();
  }
})();
"""
        return mainFrameOnly(wrapped, script.forMainFrameOnly)
    }

    private fun mainFrameOnly(source: String, forMainFrameOnly: Boolean): String {
        if (!forMainFrameOnly) {
            return source
        }
        return "if (window !== window.top) { throw new Error('Moblin user script is main frame only'); }\n$source"
    }
}
