package com.aanassar.xml

import java.util.stream.Stream
import java.util.stream.StreamSupport

import com.google.common.base.Charsets
import com.google.common.io.CharSource
import com.google.common.io.Files


File f = new File('test-files/dispatch-audit.log.2014-03-06.at.1394151936596')

// Simply wrap the concatenated XML documents in a dummy element.
CharSource compositeCharSource = CharSource.concat(CharSource.wrap("<dummy>"), Files.asCharSource(f, Charsets.UTF_8), CharSource.wrap("</dummy>"));
compositeCharSource.openBufferedStream().withReader { Reader reader ->
    new XmlSlurperIterator(reader).forEach { result ->
		println result
	}
}

compositeCharSource.openBufferedStream().withReader { Reader reader ->
	Iterable iterable = new XmlSlurperIterator(reader)
	Stream stream = StreamSupport.stream(iterable.spliterator(), false)
	def names = stream.collect { it.name() }
	println names
}