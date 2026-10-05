"""The bounded, explicit Netflix request alongside the unchanged canonical gate."""
import re

TASK_ID = 'NETFLIX_THEME'
TASK_BRANCH = 'feat/netflix-theme'
TASK_PACKET = 'tasks/NETFLIX_THEME.md'
TASK_FEATURE_IDS = [188, 189, 190, 191, 192, 195, 201, 202, 203, 205]
TASK_FIELDS = {'id', 'branch', 'task_packet', 'authorization', 'status', 'feature_ids'}


def validate_user_authorized_task(record, root):
    """Reject arbitrary branch declarations; this record authorizes one documented request."""
    if record is None:
        return []
    if not isinstance(record, dict):
        return ['user_authorized_task must be a mapping']

    errors = []
    if set(record) != TASK_FIELDS:
        errors.append('user_authorized_task requires exactly the documented task fields')
    expected = {
        'id': TASK_ID,
        'branch': TASK_BRANCH,
        'task_packet': TASK_PACKET,
        'authorization': 'explicit-user-request',
    }
    for key, value in expected.items():
        if record.get(key) != value:
            errors.append('Invalid user_authorized_task ' + key)
    if record.get('status') not in {'IN_PROGRESS', 'REVIEW'}:
        errors.append('User-authorized task must remain IN_PROGRESS or REVIEW until verified')
    feature_ids = record.get('feature_ids')
    if (not isinstance(feature_ids, list) or
            any(type(fid) is not int for fid in feature_ids) or
            sorted(feature_ids) != TASK_FEATURE_IDS):
        errors.append('User-authorized task feature IDs differ from the bounded Netflix scope')

    packet = root / TASK_PACKET
    if not packet.is_file():
        errors.append('Missing user-authorized task packet: ' + TASK_PACKET)
        return errors
    text = packet.read_text()
    # Stable metadata lines make the authorization reviewable in the task packet and state.
    metadata = {
        'Task ID': TASK_ID,
        'Branch': TASK_BRANCH,
        'Authorization': 'explicit-user-request',
        'Feature ID(s)': ', '.join(map(str, TASK_FEATURE_IDS)),
    }
    for label, expected_value in metadata.items():
        matches = re.findall(r'^' + re.escape(label) + r':\s*([^\n]+)$', text, re.MULTILINE)
        if len(matches) != 1 or matches[0].strip().strip('`') != expected_value:
            errors.append('Task packet must declare exactly one matching ' + label)
    return errors
