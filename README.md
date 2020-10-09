# The java-utils project

## Description

_java-utils_ is a collection of java utility classes used in various project at 
the National Research Council of Canada.

Many of these classes provide functionality similar to functionality found in 
frameworks like _Spring_. In such cases, the reason why we implemented our own 
version of the functionality may be that our version:
- is more lightweight and easier to use
- provides some additional features
- was implemented at a time when there was no existing alternative
- or it could just be that we just did not know that there already was a usable 
  implementation available
  
_Java-utils_ is a _Maven_ project with several sub-modules:

- _java-utils-core_: Modules that may be required by other modules. For example:
     - Acquiring configuration values from different sources (environment 
       variable, java system properties, etc.)
     - Various assertion classes for JUnit testing
     - Safe deep cloning of objects
     - Various debug and introspection utilities
     - Pretty priting JSON objects
     - File manipulation, including files that may be buried inside of JARS.
     - Temporarily caputring STDOUT to string
     
- _java-utils-data_: Classes for acquiring and manipulating data, including: 
     - Reading CSV files
     - Reading a stream of JSON objects from a file
     - File globbing
     - Searching the web using the Bing search API
     - Acquiring text from one or more web pages
     - Computing basic statistics (ex: Histogram)  
     
 - _java-utils-elasticseach_: "Streamlined" API for using Elastic Search for 
        the purposes of Natural Language Processing
    
- _java-utils-string_: Various utility classes for processing strings
     - Joining, splitting, simple tokenization
     - Diffing strings with different costing models
     
 - _java-utils-ui_: Classes for building and testing Command Line Interface 
        (CLI) or web-based UIs

## Documentation

There is no documentation per-se. However, most classes have a JUnit test case 
that starts with a section lableled "DOCUMENTATION TESTS".

Tests in this section provide well documented code examples that illustrate the 
different ways in which the class and its methods can be used. 

# Installing _java-utils_

At the moment, binary jars for _java-utils_ are not available on any 
public Maven artifactory. To use _java-utils_ in one of your projects, you must 
download it from GitHub and install it with maven command:

    mvn clean install -DskipTests=true
    
You can then include any _java-utils_ module in your project by adding an entry 
to your project's _pom.xml_ file. For example, to include module 
_java-utils-core_, you would add something like this to your pom:

		<dependency>
			<groupId>ca.nrc.java-utils</groupId>
			<artifactId>java-utils-core</artifactId>
			<version>1.0.25-SNAPSHOT</version>
		</dependency>

## _java-utils_ properties

Some of the _java-utils_ classes can be configured using properties whose names 
all start with _ca.nrc_. 

You can configure those using either:

- Environment variables
- Java JRE system variables
- The _ca_nrc_ properties file   

The first two approaches are self explanatory.

For the 3rd approach (_ca_nrc_ properties file), just create a properties file 
called _ca_nrc.properties_ and define all your _ca.nrc.*_ props in it. Then, point 
to this file using either an environment or JRE variable called _ca_nrc_.  

For example:

       
     # Using JRE variables  
     java -Dca_nrc=/path/to/your/ca_nrc.properties etc...
     
     # Using environment variable
     export ca_nrc=/path/to/your/ca_nrc.properties; java etc...
     
