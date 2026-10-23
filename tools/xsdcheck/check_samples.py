"""Validate every sample ServiceRequest in docs/ and the POC message templates against TI's real XSDs.

    python3 -I tools/xsdcheck/check_samples.py        # exit code 1 if any sample is invalid

Needs lxml (pip install lxml); the standard library cannot validate XSD.
"""
import glob
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import xsd_lib as X  # noqa: E402

SAMPLE = re.compile(r'```xml\n(.*?)```', re.S)
POC_SAMPLES = os.path.join(X.ROOT, 'poc', 'mq-channel', 'src', 'main', 'resources', 'samples')


def samples():
    """Yield (source label, operation, xml text)."""
    for path in sorted(glob.glob(os.path.join(X.ROOT, 'docs', '**', '*.md'), recursive=True)):
        with open(path, encoding='utf8') as fh:
            for m in SAMPLE.finditer(fh.read()):
                text = m.group(1)
                if '<ServiceRequest' in text:
                    op = re.search(r'<Operation>(\w+)</Operation>', text)
                    if "message body" in text:
                        continue  # an illustrative skeleton, not a sample
                    yield os.path.relpath(path, X.ROOT), op.group(1) if op else '?', text
    for template in sorted(glob.glob(os.path.join(POC_SAMPLES, '*.xml'))):
        with open(template, encoding='utf8') as fh:
            text = fh.read()
        # The POC fills these at run time; use values of the right type and length here.
        for key, value in {'TheirReference': 'POC-20261018093000-AB', 'eBankMasterRef': 'TF261018093000AB',
                           'eBankEventRef': '261018093000AB', 'TheirRef': 'MSG-20261018093000-0A1B2C3D', 'Customer': '0009990001',
                           'CustomerName': 'POC TEST CUSTOMER', 'BehalfOfBranch': '001',
                           'StartDate': '2026-10-18', 'MaturityDate': '2026-12-17'}.items():
            text = text.replace('${%s}' % key, value)
        yield os.path.relpath(template, X.ROOT), os.path.basename(template)[:-4], text


def validate():
    try:
        from lxml import etree
    except ImportError:
        raise SystemExit('check_samples.py needs lxml: pip install lxml')
    schema = etree.XMLSchema(etree.parse(os.path.join(X.schema_dir(), '_all.xsd')))
    results = []
    for source, op, text in samples():
        try:
            doc = etree.fromstring(text.encode('utf8'))
        except etree.XMLSyntaxError as e:
            results.append((source, op, ['not well-formed: %s' % e]))
            continue
        schema.validate(doc)
        errors = [re.sub(r'\{[^}]*\}', '', e.message) for e in schema.error_log]
        results.append((source, op, errors))
    return results


if __name__ == '__main__':
    bad = 0
    for source, op, errors in validate():
        print('%-5s %-10s %s' % ('ok' if not errors else 'FAIL', op, source))
        for e in errors:
            print('        ' + e)
        bad += bool(errors)
    print('%d invalid' % bad)
    sys.exit(1 if bad else 0)
