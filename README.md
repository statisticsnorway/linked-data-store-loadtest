# Linked Data Store Loadtest

Load-testing framework with GSIM inspired templates for LinkedDataStore

> For more information about Linked Data Store, please refer to the [LDS documentation](https://github.com/statisticsnorway/linked-data-store-documentation).

### Quickstart

Run a load-test by invoking one of the following scripts:
* `ldsmemory.sh`
* `ldsneo4j.sh`
* `ldspostgres.sh`

Output will be in one of the following folders after the loadtest has completed:
* `ldsmemory/results`
* `ldsneo4j/results`
* `ldspostgres/results`

### Prerequisites
* bash
* docker


### Example output

The following are examples of loadtest result output, they do not necessarily reflect current LDS performance but
is a demonstration of what sort of visual output you can expect.

##### Multiplot
![memory](doc/ldsmemory_performance_by_threads.svg)
##### Throughput
![memory](doc/ldsmemory_total_throughput.svg)
##### Read Latency (logscale)
![memory](doc/ldsmemory_read_latency.svg)
##### Write Latency (logscale)
![memory](doc/ldsmemory_write_latency.svg)
