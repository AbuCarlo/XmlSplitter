package com.aanassar.xml

import java.nio.file.Path
import java.nio.file.Paths

import com.aanassar.xml.XmlParserIterator;

def benchmark(Closure closure) {
	long start = System.currentTimeMillis()
	closure.call()
	long duration = System.currentTimeMillis() - start
	println "Took $duration ms"
}

assert args
Path inputPath = Paths.get(args[0])

benchmark {
	inputPath.toFile().withInputStream { InputStream inputStream ->

		def iterator = new XmlParserIterator(inputStream)
		iterator.eachWithIndex { node, int i ->
			println "$i: ${node.name()}"
		}
	}
}