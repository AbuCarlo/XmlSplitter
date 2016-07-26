# XmlSplitter

Iterate over huge XML documents one 2nd-level element at a time. Each such element is
returned as an instance of the Groovy class (http://docs.groovy-lang.org/latest/html/api/groovy/util/XmlParser.html) or 
(http://docs.groovy-lang.org/latest/html/api/groovy/util/XmlSlurper.html), but any
implementation of (https://docs.oracle.com/javase/8/docs/api/org/xml/sax/ContentHandler.html) is possible. Transforming each instance using an identity transform or some other XSLT
is possible with (https://docs.oracle.com/javase/8/docs/api/javax/xml/transform/sax/TransformerHandler.html), esp. the "identity transform." 
