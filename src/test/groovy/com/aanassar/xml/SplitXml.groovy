package com.aanassar.xml

import java.nio.file.Path
import java.nio.file.Paths

import javax.xml.namespace.QName
import javax.xml.stream.XMLEventWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.transform.Result
import javax.xml.transform.sax.SAXResult

import com.aanassar.xml.AbstractXmlSplitter;
import com.google.common.base.Charsets
import com.google.common.io.ByteStreams
import com.google.common.io.CharSource
import com.google.common.io.Files

class GroovySplitter extends AbstractXmlSplitter {

    @Override
    protected void nextSubtree() {
		// Implements ContentHandler...
		// validating, namespaceAware
        XmlParser parser = new XmlParser(false, true);
		// The base class provides this method.
        copySubtree parser
        // Now fetch the result again. "parent" is in fact a 
		// private instance variable, but somehow this works.
		// The alternative would be to copy the input to a
		// ByteArrayOutputStream or StringWriter, then reparse it.
		def result = parser.parent
        println result
		// See also XmlSlurper#getDocument()
    }
}

assert args
Path inputPath = Paths.get(args[0])

// Assume UTF-8 encoding. 
inputPath.toFile().withInputStream { InputStream inputStream ->
	GroovySplitter splitter = new GroovySplitter()
	splitter.splitXml inputStream
}
