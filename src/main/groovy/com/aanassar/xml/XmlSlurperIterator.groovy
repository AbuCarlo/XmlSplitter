package com.aanassar.xml

import java.io.InputStream;
import java.io.Reader;

import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NoChildren
import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.DataflowQueue

public class XmlSlurperIterator implements Iterable<GPathResult> {

	private class AsyncSplitter extends AbstractXmlSplitter implements Iterator<GPathResult> {

		private boolean isClosed;
		private final GPathResult lastResult = new NoChildren(null, "no-name", [:]);
		// We may simply want a bounded queue here.
		private final DataflowQueue queue = new DataflowQueue();
		private GPathResult nextResult;

		AsyncSplitter(input) {
			// The actor immediately begins to
			// read the XML input in the background.
			Actors.actor {
				splitXml(input);
				// Signal the end of parsing. Can we just use null?
				queue << lastResult
			}
		}

		@Override
		protected void nextSubtree() {
			// Implements ContentHandler...
			// validating, namespaceAware
			XmlSlurper slurper = new XmlSlurper(false, true);
			copySubtree slurper
			// Now fetch the result again.
			def result = slurper.document
			// This is where you should handle the result.
			queue << result
		}

		@Override
		public boolean hasNext() {
			nextResult = queue.val
			if (nextResult.is(lastResult)) {
				isClosed = true
				return false
			}
			return true
		}

		@Override
		public GPathResult next() {
			assert !isClosed
			return nextResult;
		}
	}

	private final Object input

	public XmlSlurperIterator(final InputStream inputStream) {
		this.input = inputStream
	}

	public XmlSlurperIterator(final Reader reader) {
		this.input = reader
	}

	@Override
	public Iterator<GPathResult> iterator() {
		return new AsyncSplitter(input)
	}
}
