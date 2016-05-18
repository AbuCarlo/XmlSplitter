package com.aanassar.xml

import com.google.common.base.Charsets
import com.google.common.io.CharSource
import com.google.common.io.Files

class XmlLogSplitter extends AbstractXmlSplitter {
        
    @Override
    protected void nextSubtree() {
		// Implements ContentHandler...
		// validating, namespaceAware
        XmlSlurper slurper = new XmlSlurper(false, true);
        copySubtree slurper
        // Now fetch the result again.
		def result = slurper.document
		// This is where you should handle the result.
		println result
    }
}

File f = new File('test-files/dispatch-audit.log.2014-03-06.at.1394151936596')

AbstractXmlSplitter splitter = new XmlLogSplitter()
// Simply wrap the concatenated XML documents in a dummy element.
CharSource compositeCharSource = CharSource.concat(CharSource.wrap("<dummy>"), Files.asCharSource(f, Charsets.UTF_8), CharSource.wrap("</dummy>"));
compositeCharSource.openBufferedStream().withReader { Reader reader ->
    splitter.splitXml reader
}