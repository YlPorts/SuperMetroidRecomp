#!/usr/bin/env python3
"""Reject known guest execution failures even when RUN_FRAMES exits successfully.

This inspects a desktop engine run, not an Android device or an audible output.
A clean result only means that these known log markers were absent.
"""
import argparse
import json
from pathlib import Path
import re


def inspect(log, required_frames):
    patterns = {
        'unresolved_abandon': r'\[unresolved-abandon\]',
        'watchdog_failure': r'watchdog[^\n]*(?:triggered|exceeded|timeout|hit)',
        'bridge_failure': r'(?:interp|bridge)[^\n]*(?:loop[- ]bail|timed out|timeout|cap exceeded|budget exceeded)',
    }
    failures = [
        {'line': i, 'kind': kind, 'message': line.strip()}
        for i, line in enumerate(log.splitlines(), 1)
        for kind, pattern in patterns.items()
        if re.search(pattern, line, re.IGNORECASE)
    ]
    completed = [int(v) for v in re.findall(r'exit: RUN_FRAMES reached after (\d+) frames', log)]
    finished = bool(completed and completed[-1] >= required_frames)
    return {
        'required_frames': required_frames,
        'completed_frames': completed[-1] if completed else None,
        'frame_limit_reached': finished,
        'known_execution_failures': failures,
        'passed_known_log_checks': finished and not failures,
        'scope': 'Host engine log only; no full-game, audible audio or physical Android certification',
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--log', type=Path, required=True)
    parser.add_argument('--frames', type=int, required=True)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    if args.frames < 1:
        parser.error('--frames must be positive')
    result = inspect(args.log.read_text(errors='replace'), args.frames)
    text = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
    if args.report:
        args.report.write_text(text)
    print(text, end='')
    return 0 if result['passed_known_log_checks'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
