"""Read TI's real XSDs from the jars in sources/ (never modified).

    python3 -I tools/xsdcheck/xsd_lib.py paths TFIGTAPP      # element paths of one message
    python3 -I tools/xsdcheck/xsd_lib.py extra TFIGTAPP      # its ExtraData fields with event codes

The two jars are unpacked into tools/xsdcheck/.cache/ (git-ignored) and merged into one
tree, which is how TI loads them: the customisation jar supplies services/custom/.
Walking needs only the standard library; validation (check_samples.py) needs lxml.
"""
import glob
import hashlib
import os
import sys
import zipfile
import xml.etree.ElementTree as ET

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
JARS = [os.path.join(ROOT, 'sources', 'com.misys.tiplus2.gen.ti-xsd.jar'),
        os.path.join(ROOT, 'sources', 'com.misys.tiplus2.gen.ti-xsd.customisation.jar')]
CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '.cache')
XS = '{http://www.w3.org/2001/XMLSchema}'
NS_MSG = 'urn:messages.service.ti.apps.tiplus2.misys.com'
NS_CTL = 'urn:control.services.tiplus2.misys.com'
NS_CUSTOM = 'urn:custom.service.ti.apps.tiplus2.misys.com'
WRAPPER = ('<xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" targetNamespace="urn:local.wrapper">\n'
           '  <xsd:import namespace="%s" schemaLocation="control/control.xsd"/>\n'
           '  <xsd:import namespace="%s" schemaLocation="services/services.xsd"/>\n'
           '</xsd:schema>\n' % (NS_CTL, NS_MSG))


def schema_dir():
    """Unpack both jars once (keyed by their content hash) and return the merged directory."""
    h = hashlib.sha256()
    for j in JARS:
        with open(j, 'rb') as fh:
            h.update(fh.read())
    target = os.path.join(CACHE, h.hexdigest()[:16])
    if os.path.exists(os.path.join(target, '_all.xsd')):
        return target
    os.makedirs(target, exist_ok=True)
    for j in JARS:
        with zipfile.ZipFile(j) as z:
            for name in z.namelist():
                dest = os.path.normpath(os.path.join(target, name))
                if not dest.startswith(target + os.sep):
                    raise SystemExit('unsafe path in %s: %s' % (j, name))
                if name.endswith('/'):
                    continue
                os.makedirs(os.path.dirname(dest), exist_ok=True)
                with z.open(name) as src, open(dest, 'wb') as out:
                    out.write(src.read())
    with open(os.path.join(target, '_all.xsd'), 'w') as fh:
        fh.write(WRAPPER)
    return target


def manifest_version(jar=JARS[0]):
    with zipfile.ZipFile(jar) as z:
        for line in z.read('META-INF/MANIFEST.MF').decode().splitlines():
            if line.startswith('Implementation-Version:'):
                return line.split(':', 1)[1].strip()
    return ''


class Schemas:
    """All global components of every .xsd in the merged tree, keyed by (namespace, local name)."""

    def __init__(self, base=None):
        self.base = base or schema_dir()
        self.types, self.elements, self.groups, self.files = {}, {}, {}, {}
        for path in glob.glob(os.path.join(self.base, '**', '*.xsd'), recursive=True):
            if path.endswith('_all.xsd'):
                continue
            prefixes = dict(ev[1] for ev in ET.iterparse(path, events=['start-ns']))
            root = ET.parse(path).getroot()
            tns = root.get('targetNamespace', '')
            for child in root:
                name = child.get('name')
                if not name:
                    continue
                key = (tns, name)
                if child.tag in (XS + 'complexType', XS + 'simpleType'):
                    self.types[key] = (child, prefixes, tns)
                elif child.tag == XS + 'element':
                    self.elements[key] = (child, prefixes, tns)
                elif child.tag == XS + 'group':
                    self.groups[key] = (child, prefixes, tns)

    @staticmethod
    def qname(value, prefixes, tns):
        if ':' in value:
            p, local = value.split(':', 1)
            return prefixes.get(p, ''), local
        return prefixes.get('', tns), value

    def messages(self):
        """Names of the TI transaction messages (TF...) that have a service schema."""
        return sorted(n for (ns, n) in self.elements if ns == NS_MSG and n.startswith('TF'))

    def walk(self, message):
        """Yield (path, element node, prefixes, tns) for every element under a message, depth first."""
        node, prefixes, tns = self.elements[(NS_MSG, message)]
        yield from self._element_children(node, prefixes, tns, '', 0)

    def _element_children(self, el, prefixes, tns, path, depth):
        if depth > 15:
            return
        inline = el.find(XS + 'complexType')
        if inline is not None:
            yield from self._content(inline, prefixes, tns, path, depth)
        elif el.get('type'):
            key = self.qname(el.get('type'), prefixes, tns)
            if key in self.types and self.types[key][0].tag == XS + 'complexType':
                t, p2, tns2 = self.types[key]
                yield from self._content(t, p2, tns2, path, depth)

    def _content(self, node, prefixes, tns, path, depth):
        for child in node:
            tag = child.tag
            if tag in (XS + 'sequence', XS + 'choice', XS + 'all'):
                yield from self._content(child, prefixes, tns, path, depth)
            elif tag in (XS + 'complexContent', XS + 'simpleContent'):
                yield from self._content(child, prefixes, tns, path, depth)
            elif tag == XS + 'extension':
                key = self.qname(child.get('base'), prefixes, tns)
                if key in self.types and self.types[key][0].tag == XS + 'complexType':
                    t, p2, tns2 = self.types[key]
                    yield from self._content(t, p2, tns2, path, depth)
                yield from self._content(child, prefixes, tns, path, depth)
            elif tag == XS + 'restriction':
                yield from self._content(child, prefixes, tns, path, depth)
            elif tag == XS + 'group' and child.get('ref'):
                g, p2, tns2 = self.groups[self.qname(child.get('ref'), prefixes, tns)]
                yield from self._content(g, p2, tns2, path, depth)
            elif tag == XS + 'element':
                if child.get('ref'):
                    target, p2, tns2 = self.elements[self.qname(child.get('ref'), prefixes, tns)]
                    name = target.get('name')
                else:
                    target, p2, tns2, name = child, prefixes, tns, child.get('name')
                p = (path + '/' if path else '') + name
                yield p, child, p2, tns2
                yield from self._element_children(target, p2, tns2, p, depth + 1)

    def paths(self, message, include_extra=False):
        return [p for p, *_ in self.walk(message)
                if include_extra or not (p == 'ExtraData' or p.startswith('ExtraData/'))]

    def extra_type(self, message):
        """Name of the custom type used by ExtraData, or '' if the message has none."""
        for p, node, prefixes, tns in self.walk(message):
            if p == 'ExtraData' and node.get('type'):
                return self.qname(node.get('type'), prefixes, tns)[1]
        return ''

    def extra_fields(self, message):
        """[(path under ExtraData, event code or '', xsd type)] for the message's ExtraData."""
        out = []
        for p, node, prefixes, tns in self.walk(message):
            if not p.startswith('ExtraData/'):
                continue
            code = ''
            for info in node.iter():
                if info.tag.endswith('msg.event.code'):
                    code = (info.text or '').strip()
            typ = node.get('type', '')
            if not typ:
                r = node.find('.//' + XS + 'restriction')
                typ = (r.get('base') if r is not None else '') or 'complex'
            out.append((p[len('ExtraData/'):], code, typ))
        return out


def _cli():
    if len(sys.argv) != 3 or sys.argv[1] not in ('paths', 'extra'):
        raise SystemExit(__doc__)
    s = Schemas()
    if sys.argv[1] == 'paths':
        print('\n'.join(s.paths(sys.argv[2], include_extra=True)))
    else:
        print('ExtraData type:', s.extra_type(sys.argv[2]) or 'none')
        for p, code, typ in s.extra_fields(sys.argv[2]):
            print('%-50s %-6s %s' % (p, ('c' + code) if code else '', typ))


if __name__ == '__main__':
    _cli()
