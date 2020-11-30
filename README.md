[![Build Status](https://dev.azure.com/statisticsnorway/Dapla/_apis/build/status/statisticsnorway.rawdata-converter-app-altinn3?branchName=master)](https://dev.azure.com/statisticsnorway/Dapla/_build/latest?definitionId=xxx&branchName=master)

# Rawdata Converter App - Altinn3

Rawdata Converter Application for Altinn3 data.

## Make targets

You can use `make` to execute common tasks:

```
build                          Build all and create docker image
build-mvn                      Build project and install to you local maven repo
build-docker                   Build the docker image
release-dryrun                 Simulate a release in order to detect any issues
release                        Release a new version. Update POMs and tag the new version in git
run-local                      Run the app locally (without docker)
```

## Triggering the converter

This [page](requests/README.md) gives an overview of how you can trigger the converter.