#!/usr/bin/env python3
"""Check the distributable, including ABI, certificate and 16-KiB ELF/ZIP alignment."""
import argparse
import hashlib
import pathlib
import re
import struct
import subprocess
import zipfile

p=argparse.ArgumentParser()
p.add_argument('apk',type=pathlib.Path)
p.add_argument('--sdk',type=pathlib.Path,required=True)
p.add_argument('--certificate',default='3162b5fb52ddd1606e211c3d9f54010dd114d210eefab178c1536e2d7cdf25af')
p.add_argument('--engine-library',type=pathlib.Path,
               help='Optional stripped libmain.so: reject a stale packaged engine')
a=p.parse_args(); tools=a.sdk/'build-tools'/'36.0.0'
badging=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(a.apk)],text=True)
assert "name='com.ylports.supermetroid'" in badging
assert "versionName='0.2.0'" in badging and "versionCode='20'" in badging
assert 'application-debuggable' not in badging
with zipfile.ZipFile(a.apk) as z:
    assert sorted(n for n in z.namelist() if n.startswith('lib/'))==['lib/arm64-v8a/libSDL2.so','lib/arm64-v8a/libmain.so']
    assert not any(n.lower().endswith(('.sfc','.smc','.jks','.keystore')) for n in z.namelist())
    for name in ['lib/arm64-v8a/libmain.so','lib/arm64-v8a/libSDL2.so']:
        data=z.read(name)
        if a.engine_library and name.endswith('/libmain.so'):
            assert hashlib.sha256(data).digest()==hashlib.sha256(a.engine_library.read_bytes()).digest(), 'Packaged engine differs from the verified native build'
        assert data[:5]==b'\x7fELF\x02' and struct.unpack_from('<H',data,18)[0]==183, name
        off=struct.unpack_from('<Q',data,32)[0]
        size,count=struct.unpack_from('<HH',data,54)
        loads=0
        for i in range(count):
            kind,flags,offset,va,pa,fs,ms,align=struct.unpack_from('<IIQQQQQQ',data,off+i*size)
            if kind==1:
                loads+=1
                assert align>=16384 and offset%align==va%align,(name,align)
        assert loads>0
signature=subprocess.check_output([str(tools/'apksigner'),'verify','--verbose','--print-certs',str(a.apk)],text=True)
assert a.certificate.lower() in signature.lower(), 'Signing identity differs from Alpha 2'
assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in signature
subprocess.run([str(tools/'zipalign'),'-c','-P','16','4',str(a.apk)],check=True)
print('PASS: release 0.2.0, ARM64, original Alpha 2 certificate, v2 signature, 16 KiB ELF/ZIP; no ROM or private key')
print('SHA256',hashlib.sha256(a.apk.read_bytes()).hexdigest())
if a.engine_library:
    print('ENGINE_SHA256',hashlib.sha256(a.engine_library.read_bytes()).hexdigest())
