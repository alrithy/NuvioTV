"""D066: public client configuration for the Superfork test build, and the TV QR login check.

`extract` reads an official release APK and finds the public Supabase client configuration official
compiles into it: the project URL, the anon / publishable key, a fallback URL and the TV / device
login pages. Only a key whose JWT role is `anon`, or that starts with `sb_publishable_`, is accepted;
service-role and secret keys are never accepted, kept or written. A backend candidate counts only when
it creates a TV login session with that key. The values go to a 0600 properties file and are masked in
the log; the output names categories, never values.

`session` creates one TV login session with the values compiled into a BuildConfig.java, which is
what the QR screen does when it opens.
"""
from __future__ import annotations

import argparse
import base64
import json
import os
import re
import secrets
import sys
import urllib.error
import urllib.parse
import urllib.request
import zipfile

DEFAULT_TV_LOGIN = 'https://nuvio.tv/tv-login'
DEFAULT_DEVICE_LOGIN = 'https://nuvio.tv/link'
RPC_PATH = '/rest/v1/rpc/start_tv_login_session'
DEVICE_NAME = 'Superfork test build check'

# Dex string items are NUL-terminated, so a bare origin is a whole string followed by NUL.
_ORIGIN = re.compile(rb'https://([a-z0-9.-]+\.[a-z]{2,})/?\x00')
_URL = re.compile(rb'https://[A-Za-z0-9./_-]+\x00')
_JWT = re.compile(rb'eyJ[A-Za-z0-9_-]{8,}\.eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{16,}')
_SB_KEY = re.compile(rb'sb_(?:publishable|secret)_[A-Za-z0-9_-]{8,}')


def apk_blobs(apk_path):
    with zipfile.ZipFile(apk_path) as apk:
        for name in apk.namelist():
            if re.fullmatch(r'classes\d*\.dex', name):
                yield apk.read(name)


def jwt_payload(token):
    try:
        part = token.split('.')[1]
        return json.loads(base64.urlsafe_b64decode(part + '=' * (-len(part) % 4)))
    except (IndexError, ValueError):
        return None


def classify_key(key):
    """('anon' | 'publishable', project ref or None), or None for anything that is not a public key."""
    if key.startswith('sb_publishable_'):
        return 'publishable', None
    if key.startswith('eyJ'):
        payload = jwt_payload(key)
        if isinstance(payload, dict) and payload.get('role') == 'anon':
            ref = payload.get('ref')
            return 'anon', ref if isinstance(ref, str) and re.fullmatch(r'[a-z0-9]{6,40}', ref) else None
    return None


def scan(blobs):
    origins, keys, urls, rejected = set(), [], set(), 0
    for blob in blobs:
        origins.update(m.group(1).decode() for m in _ORIGIN.finditer(blob))
        urls.update(m.group(0)[:-1].decode() for m in _URL.finditer(blob))
        for m in list(_JWT.finditer(blob)) + list(_SB_KEY.finditer(blob)):
            key = m.group(0).decode()
            kind = classify_key(key)
            if kind is None:
                rejected += 1
            elif key not in [k for k, _, _ in keys]:
                keys.append((key, kind[0], kind[1]))
    return {'origins': origins, 'keys': keys, 'urls': urls, 'rejected': rejected}


def is_supabase_host(host):
    return host.endswith('.supabase.co') or host.endswith('.supabase.in')


def candidate_origins(found, login_host='nuvio.tv'):
    """Backends worth probing: Supabase project hosts, a project ref's host, and subdomains of the login site."""
    hosts = {h for h in found['origins'] if is_supabase_host(h) or h.endswith('.' + login_host)}
    hosts.update(f'{ref}.supabase.co' for _, _, ref in found['keys'] if ref)
    return sorted(hosts, key=lambda h: (is_supabase_host(h), h))


def login_page(urls, suffix, default):
    exact = sorted(u.rstrip('/') for u in urls if u.rstrip('/').endswith(suffix))
    if default in exact:
        return default, True
    return (exact[0], False) if exact else (default, None)


def start_session(origin, key, redirect, timeout=20):
    """POST start_tv_login_session as the app does. Returns (http status or error name, code length, web host)."""
    def call(body):
        req = urllib.request.Request(
            origin.rstrip('/') + RPC_PATH, data=json.dumps(body).encode(), method='POST',
            headers={'apikey': key, 'Content-Type': 'application/json', 'Accept': 'application/json'})
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                return resp.status, resp.read(65536)
        except urllib.error.HTTPError as e:
            return e.code, b''
        except (urllib.error.URLError, OSError) as e:
            return type(e).__name__, b''

    body = {'p_device_nonce': secrets.token_urlsafe(24), 'p_redirect_base_url': redirect, 'p_device_name': DEVICE_NAME}
    status, raw = call(body)
    if status in (400, 404):  # the app's legacy retry, for a backend without p_device_name
        body.pop('p_device_name')
        status, raw = call(body)
    try:
        data = json.loads(raw) if raw else None
    except ValueError:
        data = None
    row = data[0] if isinstance(data, list) and data else data
    if isinstance(row, dict) and isinstance(row.get('code'), str) and row['code']:
        return status, len(row['code']), urllib.parse.urlsplit(str(row.get('web_url', ''))).hostname or ''
    return status, 0, ''


def choose_backend(found, redirect, probe=None):
    """(primary, fallback, key, results). Primary prefers a custom domain; the Supabase host becomes the fallback."""
    probe = probe or start_session
    results, working = [], {}
    keys = sorted(found['keys'], key=lambda k: (k[1] != 'anon', k[2] is None))
    for host in candidate_origins(found):
        origin = 'https://' + host
        for key, _, ref in keys:
            if ref and is_supabase_host(host) and not host.startswith(ref + '.'):
                continue
            status, code_len, web_host = probe(origin, key, redirect)
            results.append((host, status, code_len > 0))
            if code_len > 0:
                working[origin] = key
                break
    custom = [o for o in working if not is_supabase_host(urllib.parse.urlsplit(o).hostname)]
    direct = [o for o in working if o not in custom]
    if custom:
        primary = custom[0]
        fallback = next((o for o in direct if working[o] == working[primary]), '')
    elif direct:
        primary, fallback = direct[0], ''
    else:
        return '', '', '', results
    return primary, fallback, working[primary], results


def mask(*values):
    for v in values:
        if v:
            print(f'::add-mask::{v}', flush=True)


def summary(lines):
    path = os.environ.get('GITHUB_STEP_SUMMARY')
    text = '\n'.join(lines) + '\n'
    if path:
        with open(path, 'a') as f:
            f.write(text)
    print(text)


def write_properties(path, values):
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, 'w') as f:
        for k, v in values.items():
            if v:
                f.write(f'{k}={v}\n')


def cmd_extract(args):
    found = scan(apk_blobs(args.apk))
    mask(*(k for k, _, _ in found['keys']), *('https://' + h for h in candidate_origins(found)))
    tv_login, tv_default = login_page(found['urls'], '/tv-login', DEFAULT_TV_LOGIN)
    device_login, device_default = login_page(found['urls'], '/link', DEFAULT_DEVICE_LOGIN)
    primary, fallback, key, results = choose_backend(found, tv_login)
    kinds = sorted({kind for _, kind, _ in found['keys']})
    lines = ['## Official APK public client configuration (values not printed)',
             f'- Public keys found: {len(found["keys"])} ({", ".join(kinds) or "none"}); '
             f'non-public keys ignored: {found["rejected"]}',
             f'- Backend candidates probed: {len(results)}; creating a TV login session: '
             f'{sum(1 for r in results if r[2])}']
    for i, (host, status, ok) in enumerate(results, 1):
        kind = 'Supabase project host' if is_supabase_host(host) else 'custom domain'
        lines.append(f'  - candidate {i} ({kind}): HTTP {status}, session {"created" if ok else "not created"}')
    describe = {True: 'same as the build default', False: 'differs from the build default (used)', None: 'not found (default kept)'}
    host_kind = lambda o: 'Supabase project host' if is_supabase_host(urllib.parse.urlsplit(o).hostname or '') else 'custom domain'
    lines += [f'- `NUVIO_SUPABASE_URL`: {"found (" + host_kind(primary) + ")" if primary else "not found"}',
              f'- `NUVIO_SUPABASE_FALLBACK_URL`: {"found (Supabase project host)" if fallback else "none"}',
              f'- `NUVIO_SUPABASE_ANON_KEY`: {"found (" + next(kd for k, kd, _ in found["keys"] if k == key) + ")" if key else "not found"}',
              f'- `TV_LOGIN_WEB_BASE_URL`: {describe[tv_default]}',
              f'- `DEVICE_LOGIN_WEB_BASE_URL`: {describe[device_default]}']
    summary(lines)
    if not (primary and key):
        print('::error title=Official configuration::no public backend configuration in the official APK created a TV login session')
        return 1
    write_properties(args.out, {
        'NUVIO_SUPABASE_URL': primary, 'NUVIO_SUPABASE_ANON_KEY': key, 'NUVIO_SUPABASE_FALLBACK_URL': fallback,
        'TV_LOGIN_WEB_BASE_URL': tv_login if tv_default is False else '',
        'DEVICE_LOGIN_WEB_BASE_URL': device_login if device_default is False else ''})
    return 0


def build_config_fields(text):
    return {m.group(1): m.group(2) for m in re.finditer(r'public static final String (\w+) = "((?:[^"\\]|\\.)*)";', text)}


def cmd_session(args):
    with open(args.build_config) as f:
        fields = build_config_fields(f.read())
    url, key = fields.get('SUPABASE_URL', ''), fields.get('SUPABASE_ANON_KEY', '')
    fallback = fields.get('SUPABASE_FALLBACK_URL', '')
    redirect = fields.get('TV_LOGIN_WEB_BASE_URL') or DEFAULT_TV_LOGIN
    mask(url, key, fallback)
    if not (url and key):
        print('::error::SUPABASE_URL or SUPABASE_ANON_KEY is empty in BuildConfig')
        return 1
    lines = ['## TV QR login session (from the built APK\'s BuildConfig)']
    created = False
    for label, origin in (('SUPABASE_URL', url), ('SUPABASE_FALLBACK_URL', fallback)):
        if not origin:
            continue
        status, code_len, web_host = start_session(origin, key, redirect)
        created |= code_len > 0
        lines.append(f'- `{label}`: HTTP {status}, session {"created" if code_len else "not created"}'
                     + (f' (code length {code_len}, QR page host `{web_host}`)' if code_len else ''))
    summary(lines)
    if not created:
        print('::error title=TV QR login::no TV login session could be created with the built configuration')
        return 1
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    sub = parser.add_subparsers(dest='cmd', required=True)
    p = sub.add_parser('extract')
    p.add_argument('apk')
    p.add_argument('--out', required=True)
    p.set_defaults(func=cmd_extract)
    p = sub.add_parser('session')
    p.add_argument('--build-config', required=True)
    p.set_defaults(func=cmd_session)
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == '__main__':
    sys.exit(main())
