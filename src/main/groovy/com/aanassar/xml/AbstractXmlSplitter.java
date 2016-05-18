package com.aanassar.xml;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.TransformerFactoryImpl;

import org.springframework.util.xml.StaxUtils;
import org.xml.sax.ContentHandler;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.base.Preconditions;

public abstract class AbstractXmlSplitter {

	/**
	 * This class is simply a placeholder that can be returned through the pull
	 * API to indicate the end of a nested document. It includes no error
	 * checking or handling, because we trust the StAX implementation to do the
	 * right thing, e.g. not to call {@link getCharacters()} at all, not to call
	 * {@link nextEvent()} after {@link hasNext()} has returned "false," and so
	 * on.
	 */
	private final class InternalEndDocument implements EndDocument {
		@Override
		public int getEventType() {
			return XMLStreamConstants.END_DOCUMENT;
		}

		@Override
		public Location getLocation() {
			try {
				return xmlEventReader.peek().getLocation();
			} catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean isStartElement() {
			return false;
		}

		@Override
		public boolean isAttribute() {
			return false;
		}

		@Override
		public boolean isNamespace() {
			return false;
		}

		@Override
		public boolean isEndElement() {
			return false;
		}

		@Override
		public boolean isEntityReference() {
			return false;
		}

		@Override
		public boolean isProcessingInstruction() {
			return false;
		}

		@Override
		public boolean isCharacters() {
			return false;
		}

		@Override
		public boolean isStartDocument() {
			return false;
		}

		@Override
		public boolean isEndDocument() {
			return true;
		}

		@Override
		public StartElement asStartElement() {
			return null;
		}

		@Override
		public EndElement asEndElement() {
			return null;
		}

		@Override
		public Characters asCharacters() {
			return null;
		}

		@Override
		public QName getSchemaType() {
			return null;
		}

		@Override
		public void writeAsEncodedUnicode(Writer writer)
				throws XMLStreamException {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Same comments as above, <i>mutatis mutandis</i>. The methods that return
	 * null have been found not to matter.
	 */
	private final class InternalStartDocument implements StartDocument {
		private final XMLEvent startingEvent;

		private InternalStartDocument() {
			try {
				this.startingEvent = xmlEventReader.peek();
			} catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int getEventType() {
			return XMLStreamConstants.START_DOCUMENT;
		}

		@Override
		public Location getLocation() {
			return startingEvent.getLocation();
		}

		@Override
		public boolean isStartElement() {
			return false;
		}

		@Override
		public boolean isAttribute() {
			return false;
		}

		@Override
		public boolean isNamespace() {
			return false;
		}

		@Override
		public boolean isEndElement() {
			return false;
		}

		@Override
		public boolean isEntityReference() {
			return false;
		}

		@Override
		public boolean isProcessingInstruction() {
			return false;
		}

		@Override
		public boolean isCharacters() {
			return false;
		}

		@Override
		public boolean isStartDocument() {
			return true;
		}

		@Override
		public boolean isEndDocument() {
			return false;
		}

		@Override
		public StartElement asStartElement() {
			return null;
		}

		@Override
		public EndElement asEndElement() {
			return null;
		}

		@Override
		public Characters asCharacters() {
			return null;
		}

		@Override
		public QName getSchemaType() {
			return startingEvent.getSchemaType();
		}

		@Override
		public void writeAsEncodedUnicode(Writer writer)
				throws XMLStreamException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getSystemId() {
			return null;
		}

		@Override
		public String getCharacterEncodingScheme() {
			return null;
		}

		@Override
		public boolean encodingSet() {
			return false;
		}

		@Override
		public boolean isStandalone() {
			return false;
		}

		@Override
		public boolean standaloneSet() {
			return false;
		}

		@Override
		public String getVersion() {
			return null;
		}
	}

	private final XMLInputFactory xmlInputFactory;
	private final AtomicBoolean isStarted = new AtomicBoolean();
	private XMLEventReader xmlEventReader;
	private int level = 0;
	private final SAXTransformerFactory saxTransformerFactory;
	
	protected AbstractXmlSplitter() {
		// The only XSLT processors that will transform a StAXSource are Saxon after 9.2, 
		// and the JDK's old fork of Xalan. Newer versions of Xalan or Saxon do not work.
		// See Michael Kay's <a href="https://java.net/projects/jaxp/lists/eg/archive/2009-02/message/1">comments</a>.
		saxTransformerFactory = new TransformerFactoryImpl();

		// Note that most StAX implementations seem to work here. However, Aalto has proven to be the fastest.
		// The JDK's default implementation works, takes 30% more time. This 
		xmlInputFactory = new InputFactoryImpl();
		// Avoid memory pressure by handling chunks of text successively.
		xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, false);
		xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
	}

	/**
	 * This method delegates to {@link splitXml(Reader reader)}. It assumes that
	 * the input is encoded to UTF-8. It does not close the input.
	 * 
	 * @param inputStream
	 * @throws XMLStreamException
	 */
	public final void splitXml(InputStream inputStream) throws XMLStreamException {
		splitXml(xmlInputFactory.createXMLEventReader(inputStream));
	}

	protected final void splitXml(XMLEventReader eventReader) throws XMLStreamException {
		Preconditions.checkState(isStarted.compareAndSet(false, true), "This implementation is not reusable. You must create a new instance.");
		xmlEventReader = eventReader;
		try {
			while (xmlEventReader.hasNext()) {
				XMLEvent event = xmlEventReader.peek();
				switch (event.getEventType()) {
				case XMLStreamConstants.END_ELEMENT:
					xmlEventReader.nextEvent();
					--level;
					break;
				case XMLStreamConstants.START_ELEMENT:
					if (++level == 2) {
						nextSubtree();
						--level;
					}
				case XMLStreamConstants.START_DOCUMENT:
				case XMLStreamConstants.END_DOCUMENT:
				default:
					xmlEventReader.nextEvent();
					break;
				}

			}
		} finally {
			xmlEventReader.close();
		}

	}

	public final void splitXml(Reader reader) throws XMLStreamException {
		splitXml(xmlInputFactory.createXMLEventReader(reader));
	}

	abstract protected void nextSubtree();

	/**
	 * This class is a wrapper around the event reader, and interpolates "start"
	 * and "end" events around every 2nd-level element. Again, it does very
	 * little error checking, because we trust the StAX implementation to do the
	 * right thing, e.g. not to call next() after hasNext() has returned false.
	 */
	private final class NestedXmlEventReader implements XMLEventReader {

		boolean starting = true;
		boolean closing = false;
		boolean closed = false;

		public NestedXmlEventReader() {
			// The contain instance will decrement level to 1 when the copy
			// operation is done.
			Preconditions.checkState(level == 2,
					"The parser must be on level 2, not %d", level);
		}

		@Override
		public Object next() {
			try {
				return nextEvent();
			} catch (XMLStreamException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public XMLEvent nextEvent() throws XMLStreamException {
			if (starting) {
				starting = false;
				return new InternalStartDocument();
			}
			if (closing) {
				closed = true;
				return new InternalEndDocument();
			}

			XMLEvent event = xmlEventReader.nextEvent();
			switch (event.getEventType()) {
			case XMLStreamConstants.START_ELEMENT:
				++level;
				break;
			case XMLStreamConstants.END_ELEMENT:
				if (--level == 2) {
					closing = true;
				}
				break;
			}
			return event;
		}

		@Override
		public boolean hasNext() {
			return !closed;
		}

		@Override
		public XMLEvent peek() throws XMLStreamException {
			if (this.starting) {
				return new InternalStartDocument();
			}
			if (this.closing) {
				return new InternalEndDocument();
			}
			return xmlEventReader.peek();
		}

		@Override
		public String getElementText() throws XMLStreamException {
			return xmlEventReader.getElementText();
		}

		@Override
		public XMLEvent nextTag() throws XMLStreamException {
			return xmlEventReader.nextTag();
		}

		@Override
		public Object getProperty(String name) throws IllegalArgumentException {
			return xmlEventReader.getProperty(name);
		}

		@Override
		public void close() throws XMLStreamException {
			// Do nothing.
		}
	}

	protected final void copySubtree(final ContentHandler contentHandler) throws XMLStreamException, TransformerException {
		Result saxResult = new SAXResult(contentHandler);
		copySubtree(saxResult);
	}

	protected final void copySubtree(final Writer writer) throws XMLStreamException, TransformerException {
		copySubtree(new StreamResult(writer));
	}

	protected final void copySubtree(final Result result)
			throws XMLStreamException, TransformerException {
		XMLEventReader nestedReader = new NestedXmlEventReader();
		// Saxon cannot handle a StAXSource that encapsulates an XMLEventReader.
		XMLStreamReader streamReader = StaxUtils.createEventStreamReader(nestedReader);
		Source source = new StAXSource(streamReader);

		TransformerHandler handler = saxTransformerFactory.newTransformerHandler();
		Transformer transformer = handler.getTransformer();
		transformer.transform(source, result);
	}

	/**
	 * Copies a subtree of the XML input to an {@link XMLEventWriter} created by
	 * the caller, of which the ultimate target may be a {@link File} or
	 * {@link OutputStream}.
	 * 
	 * @param eventWriter
	 * @throws XMLStreamException
	 */
	protected final void copySubtree(final XMLEventWriter eventWriter) throws XMLStreamException {
		Preconditions.checkState(xmlEventReader != null, "This class has not been initialized properly.");
		XMLEventReader nestedReader = new NestedXmlEventReader();
		eventWriter.add(nestedReader);
		eventWriter.close();
	}
}