#!/usr/bin/env python3
"""Report actual APK sizes and their largest payload categories (standard library only)."""
import argparse
from collections import defaultdict
import json
from pathlib import Path
from zipfile import ZipFile


def report(apk):
    groups = defaultdict(lambda: [0, 0])
    with ZipFile(apk) as archive:
        for entry in archive.infolist():
            name = entry.filename
            if name.startswith('lib/'):
                category = '/'.join(name.split('/')[:2])
            elif name.startswith('assets/speech-models/'):
                category = 'speech models'
            elif name.endswith('.dex'):
                category = 'DEX code'
            else:
                category = 'resources, metadata and other assets'
            groups[category][0] += entry.compress_size
            groups[category][1] += entry.file_size
    print(f'\n{apk}: {apk.stat().st_size / 1_000_000:.2f} MB ({apk.stat().st_size / 1048576:.2f} MiB)')
    print('  Packed MB    Raw MB  Category')
    for category, (packed, raw) in sorted(groups.items(), key=lambda row: -row[1][0]):
        print(f'  {packed / 1_000_000:9.2f} {raw / 1_000_000:9.2f}  {category}')
    print('  ZIP alignment and signatures account for remaining APK bytes.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input', type=Path, nargs='?',
                        default=Path('app/build/outputs/apk/release/output-metadata.json'),
                        help='APK or Gradle output-metadata.json (defaults to release metadata)')
    args = parser.parse_args()
    if args.input.suffix == '.apk':
        apks = [args.input]
    else:
        metadata = json.loads(args.input.read_text())
        apks = [args.input.parent / entry['outputFile'] for entry in metadata['elements']]
    if not apks:
        parser.error('No APK outputs found; build the release first.')
    for apk in apks:
        report(apk)


if __name__ == '__main__':
    main()
