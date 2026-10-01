"""D066: only public keys leave the official APK, and the backend that creates a session wins."""
import base64
import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import test_build_config as tbc


def fake_jwt(payload):
    enc = lambda d: base64.urlsafe_b64encode(json.dumps(d).encode()).decode().rstrip('=')
    return f'{enc({"alg": "HS256", "typ": "JWT"})}.{enc(payload)}.{"s" * 43}'


def dex(*strings):
    # Dex string items: a length byte, the bytes, NUL.
    return b''.join(bytes([len(s) & 0x7f]) + s.encode() + b'\x00' for s in strings)


ANON = fake_jwt({'iss': 'supabase', 'ref': 'abcdefghijklmnop', 'role': 'anon'})
SERVICE = fake_jwt({'iss': 'supabase', 'ref': 'abcdefghijklmnop', 'role': 'service_role'})
SECRET = 'sb_' + 'secret_' + 'x' * 20


class TestBuildConfigTests(unittest.TestCase):
    def test_only_public_keys_are_accepted(self):
        self.assertEqual(('anon', 'abcdefghijklmnop'), tbc.classify_key(ANON))
        self.assertEqual(('publishable', None), tbc.classify_key('sb_publishable_' + 'y' * 20))
        self.assertIsNone(tbc.classify_key(SERVICE))
        self.assertIsNone(tbc.classify_key(SECRET))

    def test_scan_keeps_public_keys_and_bare_origins(self):
        found = tbc.scan([dex(ANON, SERVICE, SECRET, 'https://api.nuvio.tv', 'https://example.com/x',
                              'https://nuvio.tv/tv-login', 'https://nuvio.tv/link')])
        self.assertEqual([ANON], [k for k, _, _ in found['keys']])
        self.assertEqual(2, found['rejected'])
        self.assertIn('api.nuvio.tv', found['origins'])
        self.assertNotIn('example.com', found['origins'])
        self.assertEqual(['api.nuvio.tv', 'abcdefghijklmnop.supabase.co'], tbc.candidate_origins(found))
        self.assertEqual((tbc.DEFAULT_TV_LOGIN, True), tbc.login_page(found['urls'], '/tv-login', tbc.DEFAULT_TV_LOGIN))

    def test_custom_domain_is_primary_and_supabase_host_the_fallback(self):
        found = tbc.scan([dex(ANON, 'https://api.nuvio.tv')])
        primary, fallback, key, results = tbc.choose_backend(found, tbc.DEFAULT_TV_LOGIN, lambda o, k, r: (200, 8, 'nuvio.tv'))
        self.assertEqual(('https://api.nuvio.tv', 'https://abcdefghijklmnop.supabase.co', ANON), (primary, fallback, key))
        self.assertEqual(2, len(results))

    def test_a_backend_that_creates_no_session_is_not_used(self):
        found = tbc.scan([dex(ANON, 'https://api.nuvio.tv')])
        probe = lambda o, k, r: (200, 8, 'nuvio.tv') if 'supabase' in o else (404, 0, '')
        self.assertEqual(('https://abcdefghijklmnop.supabase.co', '', ANON),
                         tbc.choose_backend(found, tbc.DEFAULT_TV_LOGIN, probe)[:3])
        self.assertEqual(('', '', ''), tbc.choose_backend(found, tbc.DEFAULT_TV_LOGIN, lambda o, k, r: (401, 0, ''))[:3])

    def test_build_config_fields(self):
        text = 'public static final String SUPABASE_URL = "https://a.supabase.co";\n' \
               'public static final String SUPABASE_ANON_KEY = "";\n'
        self.assertEqual({'SUPABASE_URL': 'https://a.supabase.co', 'SUPABASE_ANON_KEY': ''}, tbc.build_config_fields(text))


if __name__ == '__main__':
    unittest.main()
