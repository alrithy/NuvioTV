#!/usr/bin/env python3
"""Offline v1 package/skill contract checks; reuses the governance YAML dependency.

This is a focused repository validator, not a replacement for host/schema review.
"""
import json
from pathlib import Path
import re
import sys
import yaml

ROOT = Path(__file__).resolve().parents[2]
SKILLS = ('nuvio-orchestrator', 'nuvio-status', 'nuvio-feature-port',
          'nuvio-ci-recovery', 'nuvio-handoff')
SCHEMA = 'https://agent-plugins.org/schemas/1.0.0/plugin.schema.json'


def validate(root=ROOT):
    errors = []
    try:
        manifest = json.loads((root / 'plugin.json').read_text())
        allowed = {'$schema', 'name', 'version', 'description', 'author', 'homepage',
                   'repository', 'license', 'keywords', 'extensions'}
        if set(manifest) - allowed:
            errors.append('Unknown portable manifest fields')
        if manifest.get('$schema') != SCHEMA or manifest.get('name') != 'nuvio-superfork':
            errors.append('Wrong portable schema or plugin identity')
        if not re.fullmatch(r'\d+\.\d+\.\d+', manifest.get('version', '')):
            errors.append('Version must be semver')
        if manifest.get('repository') != 'https://github.com/alrithy/NuvioTV':
            errors.append('Wrong source repository')
        extension = manifest['extensions']['com.openai']
        if extension['interface']['displayName'] != 'Nuvio Superfork':
            errors.append('Wrong display name')
        if extension.get('hooks') != './hooks/hooks.json':
            errors.append('Hook path must resolve inside plugin root')
        if any(key in extension for key in ('apps', 'mcpServers')):
            errors.append('v1 must not declare MCP/apps')
        for name in ('mcp.json', '.mcp.json', '.app.json', '.codex-plugin/plugin.json'):
            if (root / name).exists():
                errors.append('Unexpected MCP/compatibility file: ' + name)
        marketplace = json.loads((root / '.agents/plugins/marketplace.json').read_text())
        entry = marketplace['plugins'][0]
        if (marketplace['name'] != 'nuvio-personal' or len(marketplace['plugins']) != 1
                or entry['name'] != manifest['name']
                or entry['source'] != {'source': 'local', 'path': './'}):
            errors.append('Marketplace must expose root plugin exactly once')
        if entry['policy'] != {'installation': 'AVAILABLE', 'authentication': 'ON_INSTALL'}:
            errors.append('Unexpected install policy')
        hooks = json.loads((root / 'hooks/hooks.json').read_text())
        if set(hooks['hooks']) != {'SessionStart'}:
            errors.append('v1 only needs SessionStart')
        handler = hooks['hooks']['SessionStart'][0]['hooks'][0]
        if handler['type'] != 'command' or handler['command'] != 'python3 -B "${PLUGIN_ROOT}/hooks/session_start.py"':
            errors.append('Unexpected SessionStart command')
        if not (root / 'hooks/session_start.py').is_file():
            errors.append('SessionStart script missing')
        actual = {p.parent.name for p in (root / 'skills').glob('*/SKILL.md')}
        if actual != set(SKILLS):
            errors.append('Missing/unexpected v1 skills')
        for name in SKILLS:
            path = root / 'skills' / name / 'SKILL.md'
            text = path.read_text(encoding='utf-8')
            match = re.fullmatch(r'---\n(.*?)\n---\n(.+)', text, re.DOTALL)
            if not match:
                errors.append('Missing YAML frontmatter/body: ' + name)
                continue
            front = yaml.safe_load(match.group(1))
            if front.get('name') != name or not re.fullmatch(r'[a-z0-9]+(?:-[a-z0-9]+)*', name):
                errors.append('Skill name mismatch: ' + name)
            description = front.get('description')
            if not isinstance(description, str) or not 1 <= len(description) <= 1024:
                errors.append('Invalid skill description: ' + name)
            if name == 'nuvio-orchestrator' and 'اشتغل على نوفيو' not in description:
                errors.append('Missing primary Arabic trigger')
            if name == 'nuvio-status' and 'شيك لي' not in description:
                errors.append('Missing Arabic status trigger')
            for locator in ('HANDOFF', 'feature_traceability', 'MASTER_FEATURES', 'SOURCE_MAP'):
                if locator not in match.group(2):
                    errors.append('Missing live repository reference: ' + name + ':' + locator)
    except (OSError, ValueError, KeyError, TypeError, AttributeError, yaml.YAMLError) as exc:
        errors.append('Invalid/missing package resource: ' + str(exc))
    return errors


if __name__ == '__main__':
    failures = validate()
    for failure in failures:
        print(failure, file=sys.stderr)
    print('NUVIO PLUGIN STRUCTURE: ' + ('FAIL' if failures else 'PASS'))
    sys.exit(bool(failures))
