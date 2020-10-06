#The java-utils project

##Description

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

##Documentation

There is no documentation per-se. However, most classes have a JUnit test case 
that starts with a section lableled "DOCUMENTATION TESTS".

Tests in this section provide well documented code examples that illustrate the 
different ways in which the class and its methods can be used. 

