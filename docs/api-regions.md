# API Regions for the Feature Model

If you are assembling a platform (in contrast to a final application) out of several features and provide this platform for customers to build their application on top of it, additional control of the API provided by the platform is needed. The bundles within the features provide all kinds of APIs but you might not want to expose all of these as extension points. You would rather want to use some of them internally within either a single feature or share within your platform features.

## Visibility of Java API

A feature exports some API, however there are two different types of clients of the API:

* Bundles shipped as part of the platform
* Application bundles using the platform

We can generalize this by saying that API is either globally visible (to every client) or only visible to features within the same context. Usually this is referred to as a "region": The platform spawns its own region and a customer application has its own region, too. In theory there could be several customer applications running in the same framework on top of the platform, and each application has its own region.

Without any further information, API is globally visible by default. However, for platform features we want the opposite as we want to ensure that newly added API is not visible to all bundles by default.

A feature can have an additional extension of type JSON named `api-region`. The following example exposes some packages to the global region and an additional package to the platform region. Exports declared earlier in the api-regions array also apply to later elements in the array, so the `platform` region also contains all exports declared for the `global` region.

Note that the `global` region is a predefined region that exports the listed packages to everyone. Other region names can be chosen freely. Packages listed in these other regions are only exposed to bundles in features that are in the same region.

``` json
    "api-regions:JSON|optional" : [
        {
            "name": "global",
            "exports": [
                "# Export Sling's resource API in the global region",
                "org.apache.sling.resource.api",
                "org.apache.sling.resource.api.adapter",
                "org.apache.sling.resource.api.auth",
                "org.apache.sling.resource.api.request",
                "org.apache.sling.resource.api.resource"
            ]
        },{
            "name": "platform",
            "exports": [
                "# Export the scheduler API in the platform region.",
                "# All exports in earlier regions defined here also apply.",
                "org.apache.sling.commons.scheduler"
            ]
        }
    ]
```

Of course the above mentioned packages need to be exported by some bundle within the feature. By exporting packages to a given region, a feature automatically also sees all packages available to that region (or regions).

A feature can also just consume packages from a region, without having to export any packages to it. This can be done by exporting an empty list of packages. For example:

``` json
    "api-regions:JSON|optional" : [
        {
            "name": "platform",
            "exports": []
        }
    ]
```

If the api-regions extension is missing or the api-regions information is missing, it is assumed that all packages are exported to the "global" region and all packages in the global region are visible to the feature.

If a feature exports no packages and only wants to have visibility of packages from the global region, this can be specified as follows:

``` json
    "api-regions:JSON|optional" : [
        {
            "name": "global",
            "exports": []
        }
    ]
```

## Java API Behind a Toggle

API Regions support the usage of toggles, where API can be hidden behind a toggle and that API is only available in the region, if the toggle is enabled. For this, a package in a region can be configured with a toggle. In this case an object is used per package:

``` json
    "api-regions:JSON|optional" : [
        {
            "name": "global",
            "exports": [
                {
                    "name" : "org.apache.sling.api",
                    "toggle" : "SLING_API"
                }
            ]
        }
    ]
```

In the above example, the package *org.apache.sling.api* is only available if the toggle *SLING_API* is enabled.

While the above example enables a complete package based on a toggle, it is also possible to enable a new version of an existing API with a toggle:

``` json
    "api-regions:JSON|optional" : [
        {
            "name": "global",
            "exports": [
                {
                    "name" : "org.apache.sling.api",
                    "toggle" : "NEW_SLING_API",
                    "previous-artifact-id" : "org.apache.sling:org.apache.sling.api:1.1"
                }
            ]
        }
    ]
```

In the above example, if the toggle *NEW_SLING_API* is enabled, the package *org.apache.sling.api* is available based on the artifact in the feature model providing this package. If the toggle *NEW_SLING_API* is not enabled, the package *org.apache.sling.api* is available based on the artifact mentioned in the `previous-artifact-id` property. Usually that points to an older version of the API.

## Deprecation of Java API

The usual process for deprecating Java API is to mark it with a corresponding annotation and javadoc tag. However, in cases where changing the source code of an API is not possible to add such a marker, the deprecation can be specified through an API region. This information can be used by tooling to mark and report usage of such API.

``` json
     "api-regions:JSON|false":[
        {
            "name":"global",
            "exports":[
                # Package exported without deprecation
                "org.apache.sling.api",
                # Package exported - full package deprecated
                {
                    "name" : "org.apache.sling.api.file",
                    "deprecated" : "Deprecation message"
                },
                # Package exported - single class and single member deprecated
                {
                    "name" : "org.apache.sling.api.io",
                    "deprecated" : {
                        "members" : {
                            "FileCache" : "Deprecation message",
                            "MemoryCache#getFile()" : "Deprecation message"
                        }
                    }
                },
                # Instead of just the message, additional information about when the
                # deprecation happened can be provided (this works in all of the
                # above places)
                {
                    "name":"org.apache.sling.incubator.api",
                    "deprecated":{
                        "msg":"This is deprecated",
                        "since":"Since Sling left the incubator",
                        "for-removal":"2029-12-31"
                    }
                }
            ]
        }
     ]
```

The deprecation information can just be the message, or it can also include information when the deprecated started (since) and by when the member is expected to be removed (for-removal). The removal information should be either the string `true` or a date in the format `YYYY-MM-DD`.

Deprecation of single members of a package can be done by specifying a deprecation message per member as shown in the example above. The key in the `members` array specifies the member to be deprecated. It follows this format:

* Single name (not containing a hash sign): a type (class, interface) is deprecated, e.g. FileCache
* Name followed by a hash sign followed by another single name : Field in that type is deprecated, e.g. MemoryCache#TYPE
* Name followed by a hash sign followed by another name with brackets : Method in that type is deprecated, e.g. MemoryCache#getFile() or MemoryCache#getFile(String)
* Name followed by a hash sign followed by brackets : Constructor in that type is deprecated, e.g. MemoryCache#(String)
* Name followed by a hash sign followed by a dollar sign followed by a name : Inner type is depreacted, e.g. MemoryCache#$CacheKey

In addition a mode can be specified for a message, values are LENIENT (default) or STRICT. This mode is used by the analyser to decide whether a warning (LENIENT) or error (STRICT) should be issued if a deprecated package is used.

## OSGi Configurations

Apart from defining the Java API surface, for some applications it is beneficial to describe the OSGi configuration surface, too. For example, a framework might not allow an application to set some configurations or update existing configurations.

While the [OSGi metatype specification](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.metatype.html) allows to specify some type information for OSGi configurations, it does not allow to specify validation rules or differentiate between a private and a public configuration or configuration property.

### The OSGi Configuration API

The OSGi configuration API allows to define specific rules for configurations down to individual properties. The API definition is an extension of the feature model named `configuration-api`. For each configuration and factory configuration, the allowed list of properties can be specified together with a type and additional validation rules (like a range or a regular expression). All properties not mentioned for a configuration are considered internal. In addition, configuration PIDs and factory configuration PIDs can be specified as internal, too.

A similar mechanism exists for framework properties.

### Validation Mode

The configuration API establish the public API surface for OSGi configurations. It defines which configurations are allowed to be created, updated - and for such configurations which properties are allowed together with validation rules.

The validation mode defines the behaviour of the validation in case of an invalid value. It can be set globally for the configuration API and might be overwritten for single configurations or single properties. The modes are:

* `STRICT` : This is the default mode. If the validation fails, issue an error.
* `LENIENT` : If validation fails, issue a warning. The invalid value is used.
* `SILENT` : If validation fails, do not report it. The invalid value is used.
* `DEFINITIVE` : If validation fails, issue a warning. If a default value is provided, use the default value instead. If no default value is provided and the value is an array or a collection and includes or excludes are provided, these will be applied. Otherwise, the property is removed from the configuration.
* `SILENT_DEFINITIVE` : If validation fails, do not report it. If a default value is provided, use the default value instead. If no default value is provided and the value is an array or a collection and includes or excludes are provided, these will be applied. Otherwise, the property is removed from the configuration.

``` json
"configuration-api:JSON" : {
    "mode" : "SILENT",
    ...
}
```

### Configurations

Each OSGi configuration that is part of the configuration API (and therefore public) is listed below the `configurations` object with the PID of the configuration as the key. Each configuration can have the following properties:

* `title` : A human readable title
* `description` : A human readable description
* `properties` : An object containing all properties that are allowed to be configured
* `deprecated` : If this configuration should not be used anymore a human readable message.
* `since` : Info about when the configuration restriction started. It will be appended at the end of every validation message.
* `enforce-on` : Info about by when the configuration restriction is expected to be enforced (enforced-on). It will be appended at the end of every validation message.
* `mode` : Validation mode for the configuration overriding the global one. This mode applies to all properties.
* `region` : Optional property to restrict the configuration to the internal region if this is set to "INTERNAL". With this set, configurations for the internal region can be validated.
* `allow-additional-properties` : Optional property. If set to true, additional properties not listed in the description are allowed.
* `internal-property-names` : Specify property names which are internal.

``` json
"configuration-api:JSON" : {
    "configurations" : {
        "org.apache.sling.engine.impl.SlingMainServlet" : {
            "title" : "Apache Sling Main Servlet",
            "description" : "The configuration of the main servlet...",
            "properties" : {

            }
        }
    }
}
```

### Properties

For each property a JSON object contains additional information about this property. The following keys can be set:

* `type` : The type of the property, defaults to `STRING`. The following types are supported:
  * STRING : A string value
  * LONG : A long value
  * INTEGER : An integer value
  * SHORT : A short value
  * CHARACTER : A single character
  * BYTE : A byte value
  * DOUBLE : A double value
  * FLOAT : A float value
  * BOOLEAN : A boolean value, either true or false
  * PASSWORD : A string containing a password/secret
  * URL : A string containing a URL
  * EMAIL : A string containing an email address
  * PATH : A string containing a unix-style path (starts with a slash)
* `cardinality` : Single value property or multi value. Defaults to 1. If set to -1, the number of values is unlimited. Otherwise the value must be greater than zero and indicates the maximum number of values.
* `required` : Boolean, whether the property is required and needs a configuration value.
* `title` : A human readable title
* `description` : A human readable description
* `deprecated` : If this configuration should not be used anymore a human readable message.
* `includes` : An array of values. If configured, these values must be present.
* `excludes` : An array of values. If configured, these values must not be present.
* `regex` : A regular expression to validate the value.
* `range` : An object which can have a `min` and/or a `max` property to further specify the value range.
* `options` : An array of objects acting as an enumeration for the allowed values. Each option must have a `value` property. It might also have a `title` or `description` property.
* `default` : A default value which might be used depending on the validation mode.
* `mode` : Validation mode for the property overriding the global one or one set for the configuration.
* `placeholder-policy` : The placeholder policy defines whether a placeholder is allowed or required for a property. With `DEFAULT` the policy of the property type is used. `ALLOW`, `REQUIRE` , or `DENY` can be used to override that.
* `placeholder-regex` : A regular expression to validate the value, used if the value is expressed as a placeholder.

``` json
"configuration-api:JSON" : {
  "configurations" : {
        "org.apache.sling.engine.impl.SlingMainServlet" : {
            "properties" : {
                "flag" : {
                    "type" : "BOOLEAN"
                },
                "output" : {
                    "options" : [
                        {
                            "title" : "Output to text file",
                            "value" : "TEXT"
                        },
                        {
                            "title" : "Output to console",
                            "value" : "CONSOLE"
                        }
                    ]
                },
                "number" : {
                    "type" : "INTEGER",
                    "range" : {
                        "min" : 5,
                        "max" : 50
                    }
                },
                "array_of_urls" : {
                    "type" : "URL",
                    "cardinality" : -1,
                    "includes": ["https://sling.apache.org"],
                    "excludes" : ["https://outdated.apache.org"]
                }
            }
        }
    }
  }
```

### Factory Configurations

OSGi factory configurations are similarly described in the configuration API. If a factory configuration is part of the public API, it should be listed below the `factory-configurations` property using the factory PID as a key.

``` json
"configuration-api:JSON" : {
    "factory-configurations" : {
        "org.apache.sling.event.jobs.QueueConfiguration" : {
            "properties" : {

            },
            "internal-names" : ["main"],
            "operations" : ["CREATE"]
        }
    }
}
```

As with configurations, each public property needs to be described. Factory configurations support two additional properties:

* `internal-names` : A list of factory configuration names that are not public and neither can't be updated or created.
* `operations` : A list of operations out of `CREATE` and `UPDATE` - both are set by default. If `CREATE` is set, new factory configurations are allowed to be created. If `UPDATE` is set, existing configurations are allowed to be updated. But in both cases `internal-names` needs to be respected.

### Framework Properties

Similar to properties of OSGi configurations, framework properties can be described, providing validation for such properties:

``` json
"configuration-api:JSON" : {
    "framework-properties" : {
        "org.apache.sling.http.port" : {
            "type" : "INTEGER"
        }
    }
}
```

A framework property has the same configuration options as a property inside a configuration.

### Internal Configurations

Some OSGi configurations and factory configurations are not part of the public API and cannot be created/updated by application configuration. Same applies to framework properties. For configurations and factory configurations, define a configuration description without any properties. Internal framework properties are listed with just their names:

``` json
"configuration-api:JSON" : {
    "configurations" : {
        "org.apache.sling.engine.impl.InternalRequestHandling" : {
            // no properties
        }
    },
    "factory-configurations" : {
        "org.apache.sling.engine.impl.InternalLogger" : {
            // no properties
        }
    },
    "internal-framework-properties" : {
        // list of property names
    }
}
```

Note, if you specify `allow-additional-properties` to be true for configurations without properties, the configurations are not marked as internal anymore and can be used, for example as a marker configuration.

### OSGi Configuration Regions

Two regions are supported for OSGi configurations, internal and global. Without any further information, a feature is considered to be part of the global configuration region. If a feature wants to be part of the internal region it can specify this in the extension:

``` json
"configuration-api:JSON|optional" : {
  "region" : "INTERNAL"
}
```

When two features are aggregated, the resulting feature is only in the internal region if both source features are in the internal region. Otherwise, the resulting aggregate is always in the global region.

### Configuration Additions

If two features are merged, only one feature is allowed to have a configuration description for a given PID or factory PID. To enable use cases where a feature wants to enhance an existing description, additions can be specified in the configuration API. It is allowed to add additional include values and for factory confgirations additional internal configuration names.

``` json
"configuration-api:JSON|optional" : {
  "configuration-additions" : {
    "org.apache.sling.engine.RequestHandling" : {
      "properties":{
        "someprop" : {
          "includes" : ["additional-include"]
        }
      }
    }
  },
  "factory-configuration-additions" : {
    "org.apache.sling.engine.impl.InternalLogger" : {
      "internal-names" : ["additional-name]"
    }
  }
}
```

## Artifact Rules

The artifact rules extension allows to specify version rules for bundles and artifacts. For an artifact identity allowed and denied version ranges can be specified. A version range follows the OSGi version range syntax. If no ranges are specified, the artifact is not allowed. An artifact version must match at least one allowed version range and must not match any denied version range (if specified).

``` json
"artifact-rules:JSON|optional" : {
  "mode" : "INTERNAL",
  "bundle-version-rules":[
      {
          "artifact-id" : "g:a:1", # version does not matter
          "message":"Use at least version 2.0.4 but avoid 2.1.1",
          "allowed-version-ranges":["[2.0.4,3)"],
          "denied-version-ranges":["[2.1.1,2.1.1]]
      }
  ],
  "artifact-version-rules":[
      {
          "artifact-id" : "g:a:1", # version does not matter
          "message":"Use at least version 2.0.4 but avoid 2.1.1",
          "allowed-version-ranges":["[2.0.4,3)"],
          "denied-version-ranges":["[2.1.1,2.1.1]]
      }
  ]
}
```

The mode, either LENIENT or STRICT (default) can be used to decide whether a warning or an error should be emitted.
