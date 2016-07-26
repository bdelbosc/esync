# esync a tool to compare Nuxeo repository and Elasticsearch content

When using [nuxeo-elasticsearch](http://doc.nuxeo.com/x/iYElAQ) we
want to be sure that the repository content is in sync with the content
indexed in Elasticsearch.

This tool enables to detect difference between the Nuxeo database
repository and the indexed content in Elasticsearch.

# Install

## Download

Download the `nuxeo-esync-VERSION-capsule-full.jar` from [https://maven.nuxeo.org](https://maven.nuxeo.org/nexus/#nexus-search;gav~~nuxeo-esync~~~capsule-full).

## Building from sources

Create the all in one jar:

    mvn package

The jar is located here:

    ./target/nuxeo-esync-VERSION-capsule-full.jar

## QA results

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=tools_esync)](https://qa.nuxeo.org/jenkins/job/tools_esync/)

# Usage

## Configuration

Create a `/etc/esync.conf` or `~/.esync.conf` using one of the samples provided :
- `esync-postgresql.conf.example`
- `esync-mssql.conf.example`
- `esync-mongodb.conf.example`

You will need to configure the database and Elasticsearch access.

Refer to the [source for the full list of options](https://github.com/bdelbosc/esync/blob/master/src/main/java/config/ESyncConfig.java)
available.

## Invocation

     # using a default conf located in /etc/esync.conf or ~/.esync.conf
     java -jar /path/to/nuxeo-esync-$VERSION-capsule-full.jar

     # using an another config file
     java -jar /path/to/nuxeo-esync-$VERSION-capsule-full.jar /path/to/config-file.conf

     # customizing the log
     java -Dlog4j.configuration=file:mylog4j.xml -jar nuxeo-esync-$VERSION-capsule-full.jar


You can find the default [log4.xml here](https://github.com/bdelbosc/esync/blob/master/src/main/resources/log4j.xml)
default log file is in `/tmp/trace.log`.


# Checkers

The tool runs concurrently different checkers.

Checkers compare the reference database aka **expected** with the Elasticsearch content aka **actual**.

They report different things:

- Errors like a different number of documents, total or per document type
- Missing or spurious document types in Elasticsearch
- Missing documents ids in Elasticsearch
- Spurious documents ids in Elasticsearch
- Difference in document properties like ACL, path...


Here is a list of available checkers.

## Cardinality Checker

This is a quick check to count the total number of documents in the db and Elasticsearch.
There are 4 document counts:
- documents without version and proxy
- version documents
- proxy documents
- orphan documents other than version

False positive cases:
- this does not garantee that we have the same documents indexed, just the same number.

False negative cases:
- some system documents are not indexed (like CommentRelation or PublicationRelation)

## Type Cardinality Checker

Checks the number of each document types for documents and versions

False positive cases:
- this does not guarantee that we have the same documents indexed, just the same number for a primary type.

False negative cases:
- some system documents are not indexed and reported as missing type

## Type Document Lister

When there is a difference raise by the Type Cardinality checker the list of ids for this type
is compared, to gives the missing and spurious document ids.

False positive cases: None
False negative cases: None

It can takes time and memory to list all doc ids from the database.

## ACL Checker

It performs 2 checks:
- Checks that all documents that hold an ACL are well indexed in ES
- Checks that all documents in ES have a correct ACL

False positive cases:
- some ACL can be more permissive on ES

False negative cases:
- none

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
