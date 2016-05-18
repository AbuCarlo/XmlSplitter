package com.aanassar.xml;

import groovyx.gpars.actor.Actors

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

public final class XmlParserIterator implements Iterable<Node> {
	
	private final Node lastNode = new Node(null, 'dummy', [:])

	private class InternalSplitter extends AbstractXmlSplitter implements Iterator<Node> {
		// The blocking queue has a maximum capacity of 12, which is obviously the 
		// sort of magic number that's considered bad style.
		private final BlockingQueue<Node> queue = new LinkedBlockingQueue<Node>(12)
		// I don't remember why I implemented it this way. We should only need a
		// single reference variable which can be set to null.
		private final Deque<Node> drain = new LinkedList<Node>()
		private boolean closed = false

		InternalSplitter(input) {
			Actors.actor {
				splitXml(input);
				queue.put lastNode
			}
		}

		@Override
		protected void nextSubtree() {
			XmlParser parser = new XmlParser(false, true);
			copySubtree parser
			Node result = parser.parent
			queue.put result
		}

		@Override
		public boolean hasNext() {
			if (this.closed) {
				return false;
			}
			if (!this.drain.isEmpty()) {
				return true
			}
			// Block.
			Node result = queue.take()
			if (result.is(lastNode)) {
				this.closed = true;
				return false;
			}
			this.drain.offer(result)
			return true
		}

		@Override
		public Node next() {
			assert !this.closed && !this.drain.isEmpty()
			return this.drain.remove()
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException()
		}
	}
	
	private final Object input 

	public XmlParserIterator(final InputStream inputStream) {
		this.input = inputStream
	}

	public XmlParserIterator(final Reader reader) {
		this.input = reader
	}

	@Override
	public Iterator<Node> iterator() {
		return new InternalSplitter(input);
	}
}
