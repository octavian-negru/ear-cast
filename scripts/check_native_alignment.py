#!/usr/bin/env python3
"""Check every 64-bit ELF LOAD segment in an APK/AAB for 16 KB support.

This checks ELF alignment, not APK ZIP alignment or runtime behavior. Also run
zipalign on generated APKs and test on a device booted with 16 KB pages.
"""
import argparse
import struct
import zipfile


def check_elf(data):
    if data[:4] != b'\x7fELF':
        raise ValueError('Not an ELF library')
    if data[4] != 2:
        return None  # The Play page-size requirement concerns 64-bit ABIs.
    endian = {1: '<', 2: '>'}.get(data[5])
    if endian is None:
        raise ValueError('Invalid ELF byte order')
    phoff = struct.unpack_from(endian + 'Q', data, 32)[0]
    size, count = struct.unpack_from(endian + 'HH', data, 54)
    if size < 56 or not count or phoff + size * count > len(data):
        raise ValueError('Invalid ELF program headers')
    loads = []
    for index in range(count):
        fields = struct.unpack_from(endian + 'IIQQQQQQ', data, phoff + index * size)
        kind, _, offset, address, _, _, _, alignment = fields
        if kind == 1:
            loads.append(alignment)
            if alignment < 16384 or alignment & (alignment - 1) or (address - offset) % 16384:
                raise ValueError(f'LOAD segment incompatible with 16 KB pages (alignment={alignment})')
    if not loads:
        raise ValueError('No LOAD segments')
    return min(loads)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('archive', help='APK or AAB to inspect')
    args = parser.parse_args()
    checked = 0
    failures = []
    with zipfile.ZipFile(args.archive) as archive:
        for name in sorted(archive.namelist()):
            if '/lib/' not in '/' + name or not name.endswith('.so'):
                continue
            try:
                alignment = check_elf(archive.read(name))
                if alignment is not None:
                    checked += 1
                    print(f'PASS {name}: LOAD alignment {alignment}')
            except (ValueError, struct.error, IndexError) as error:
                failures.append(f'{name}: {error}')
    if not checked:
        failures.append('No compatible 64-bit native libraries found')
    if failures:
        raise SystemExit('\n'.join(failures))
    print(f'Checked {checked} 64-bit native libraries. ZIP alignment and device tests remain separate checks.')


if __name__ == '__main__':
    main()
