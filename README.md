[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=Sling/sling-org-apache-sling-feature-extension-apiregions/master)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-feature-extension-apiregions/job/master) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/job/Sling/job/sling-org-apache-sling-feature-extension-apiregions/job/master.svg)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-feature-extension-apiregions/job/master/test_results_analyzer/) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/feature.md)

# Apache Sling API Regions extension

This component contains extensions relating to the API Regions component. For more information about API Regions see
https://github.com/apache/sling-org-apache-sling-feature/blob/master/apicontroller.md

The following extensions are registered via the ServiceLoader mechanism:

## `org.apache.sling.feature.builder.MergeHandler`
Merge handlers are called when features are merged during the aggregation process.

`APIRegionMergeHandler` - This handler knows how to merge API Regions extensions
