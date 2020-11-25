[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-extension-apiregions/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-extension-apiregions/job/master/)&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-extension-apiregions/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-extension-apiregions/job/master/test/?width=800&height=600)&#32;[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-extension-apiregions&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-extension-apiregions)&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-extension-apiregions&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-extension-apiregions)&#32;[![JavaDoc](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.feature.extension.apiregions.svg)](https://www.javadoc.io/doc/org.apache.sling/org-apache-sling-feature-extension-apiregions)&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.feature.extension.apiregions/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.feature.extension.apiregions%22)&#32;[![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/group/feature.md) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling API Regions Extension for the Feature Model

This component contains extensions relating to the API Regions component.
Read the documentation about [API Regions](docs/api-regions.md) for more information.

## Feature Model Analysers

This component also contains Feature Model Analysers they are contributed through the Service Loader mechanism to the set of Analysers.

Documentation can be found here: https://github.com/apache/sling-org-apache-sling-feature-analyser . These can be run as part of the 'analyse-features' goal with the [slingfeature-maven-plugin](https://github.com/apache/sling-slingfeature-maven-plugin#analyse-features-analyse-features).  

These analysers relate to API Region definitions in Feature Models.

* `api-regions`: This analyser ensures that packages listed as exports in API-Regions sections are actually exported by a bundle that's part of the feature.

* `api-regions-dependencies`: This analyser checks that packages in API regions listed earlier in the API-Regions declaration have no dependency on API regions listed later in the list. This include `Import-Package` style dependencies and also uses-clause type dependencies. Later API regions also include packages from earlier declared API regions, but not the other way around.
  * Configuration parameters:
  * `exporting-apis`: the name of the region that provides the visible APIs.
  * `hiding-apis`: the name of the region that is 'hidden' i.e. not as visible as the exporting one. The
packages in the `exporting-api` cannot depend on any packages from this region.

* `api-regions-duplicates`: This analyser ensures that packages are only listed in one region
in a given feature. If the same package is listed in multiple regions this will be an error.

* `api-regions-exportsimports`: Checks bundle import/export package statements for consistency and completeness. If API Regions are used this analyser includes this 
information as part of the check, to ensure that bundles don't import packages of which they have no visibility because of API Regions restrictions.

* `api-regions-check-order`: This analyser checks that regions are defined in the specified
order and that the same region is only declared once. Later regions inherit the packages
expose in earlier regions in the list, so the order is important.
  * Configuration parameters:
  * `order`: A comma separated list of the region names declaring the order in which they should be found. Not all regions declared must be present, but if they are present this
order must be obeyed.

* `api-regions-crossfeature-dups`: This analyser checks whether the same package is exported 
into the same API Region from multiple features. It can prevent against unwanted results when packages are exported by a bundle in a platform feature in an API Region such as `global` as well as by a non-platform bundle.
This analyser only provides a useful result when run on
an aggregate feature model, i.e. a feature model that was created by aggregating a number of other feature models. It uses the
`feature-origins` metadata to find the features that bundles were initially declared in. It then matches this with the `feature-origins` found in the `api-regions` section. Exports from  bundles from features that don't
declare `api-regions` are compared to declared exports in the `api-regions` section. If there is overlap an error
is reported.
  * Configuration parameters:
  * `regions`: a comma separated list of regions to check. If not specified all regions found are checked. This configuration item can be used to exclude certain regions from the check.
  * `definingFeatures`: comma separated list the features IDs that are allowed to define the API. If overlapping exports
are done into the selected regions from other features this will cause an error. The suffix `*` is
supported as a wildcard at the end of the feature ID. If this configuration is not specified, the list of defining features
is built up from all features that opt-in to the API regions and ones that
don't opt-in are assumed to be non-defining.
  * `warningPackages`: if packages listed here are found to overlap, a warning instead of an error is reported. Supports either literal package names (e.g. `javax.servlet`) or wildcards with an asterisk at the end (e.g. `javax.*`).
  * `ignoredPackages`: packages listed here are completely ignored in the analysis. Supports literal package names or wildcards with an asterisk at the end.

## Extensions

The following extensions are registered via the ServiceLoader mechanism:

## `org.apache.sling.feature.builder.MergeHandler`
Merge handlers are called when features are merged during the aggregation process.

`APIRegionMergeHandler` - This handler knows how to merge API Regions extensions


# Additional Extensions

The following extensions are also implemented by this component and made available through the Service Loader mechanism:

* org.apache.sling.feature.launcher.spi.extensions.ExtensionHandler
* org.apache.sling.feature.launcher.spi.Launcher
* org.apache.sling.feature.scanner.spi.ExtensionScanner
