Introduction
============

Reasons for SIRIUS
------------------

The SIRIUS framework was built by scireum_ as platform for our web based applications. Existing frameworks didn't quite
cater our needs concerning flexibility, reusability and simplicity.

.. _scireum: http://www.scireum.de

So here's whats provided by SIRIUS:

* micro kernel based dependency injector
* powerful configuration framework (based on `typesafe config <https://github.com/typesafehub/config>_`)
* modern high performance web server (based on `netty <http://netty.io>_`)
* flexible way of handling customer based customizations
* engine for providing REST services via XML and JSON
* extensive monitoring and profiling support

Using SIRIUS
------------

Using SIRIUS in your own application is quite easy. Our approach is to include it as Git submodule. It therefore
becomes a subdirectory in your project. The makes updates extremely easy. Also you have all required build files
and supporting libraries along with the whole source code at hands. Depending on which profiles you use, add the
appropriate **src**, **resources** and **test** directories as source code roots to your project. Also add
**build/lib** as location for libraries or jars. This will be populated by Ivy using the **build.xml** as shown below.

Next to this, also add **sirius/build/unmanaged-lib** and if you're using the *App Profile*
**sirius/build/unmanaged-app-lib** as library path to your project.

The easiest way to do all this is to use **IntelliJ** as we provide project files for each profile which simply can
be included as module in your project.

In order to make the dependency injector aware of your own classes, place a *component.marker* in your **src** or
**resources** folder.

Profiles
--------

SIRIUS is split into four profiles: **kernel**, **web**, **app**, **search**. The idea is to limit the number of
dependencies required if only a part of the functionality is required.

Kernel profile
^^^^^^^^^^^^^^
The kernel provides common helper classes, the dependency injector as well as the configuration framework and is
required by all other profiles.

Web profile
^^^^^^^^^^^
The web profile provides the built in web server, the template and scripting system as well as the security framework
and a JDBC access layer.

App profile
^^^^^^^^^^^
The app profile provides various integration components (therefore brings some dependencies) as well as a basic
implementation of the Servlet Specification which permits the run servlets and JSF components within the
web server. This is largely intended to support legacy code and not as a first class servlet container.

Search profile
^^^^^^^^^^^^^^
The search profile provides a "ORM" layer for `Elasticsearch <http://www.elasticsearch.org>_`.

Directory Layout
----------------

SIRIUS contains one directory for each profile as well as the **build** directory. This contains the required Ant
build base file as well as the Ivy files used to download all required dependencies. Additionally the two sub
directories **unmanaged-lib** and **unmanaged-app-lib** contain libraries which cannot be fetched via Ivy.

Each profile contains the sources split up into **src**, **test** and **resources**. Additionally there is a **dist**
directory. Contents within this directory are directly copied into the release directory. Therefore the **kernel**
provides a startup script as well as a Windows Service Wrapper.

The main application (using SIRIUS) should have a **build** directory. This has to contain the main build file
**build.xml** as well as the main Ivy file **ivy.xml**. Next to them you can provide an **unmanaged-lib** directory
which can contain further dependencies used by the application which are not available via Ivy.

build.xml
^^^^^^^^^

This file is used to build the product and setup the environment. Therefore create a **build.xml** within the
**build** folder which contains the contents as shown below. Make sure the enable or disable the profiles as
required and setup the name of the target application in **app.name** and **app.filename**. Then run the target
**ivy** to fetch all required dependencies.

.. code-block:: xml

    <project name="PRODUCT-NAME" default="ivy">

        <import file="../sirius/build/build-base.xml" />
        <!-- Uncomment to disable the web profile -->
        <!-- <property name="no-web" value="true" /> -->
        <!-- Uncomment to disable the search profile -->
        <!-- <property name="no-search" value="true" /> -->
        <!-- Uncomment to disable the app profile -->
        <!-- <property name="no-app" value="true" /> -->

        <!-- Name of the product -->
        <property name="app.name" value="MyProduct" />
        <!-- Technical of the product which can be used as filename -->
        <property name="app.filename" value="my-product" />
        <!-- If you're using a CI tool like Teamcity or Jenkins, you can pass on the build number and VCS infos: -->
        <!-- <property name="app.build" value="${build.number}" /> -->
        <!-- <property name="app.vcs" value="${build.vcs.number}" /> -->

    </project>

ivy.xml
^^^^^^^

SIRIUS uses Ivy to fetch all required dependencies. Each profile brings a set of dependencies which are listed in
a separate XML file. As Ivy has no notion of modules one has to include these files in the main **ivy.xml**
which is to be created in the **build** directory.

.. code-block:: xml

    <?xml version="1.0"?>
    <!DOCTYPE ivy-module [
            <!-- Remove dependencies for profiles not required by the application -->
            <!ENTITY kernel SYSTEM "../sirius/build/ivy-kernel.xml">
            <!ENTITY web SYSTEM "../sirius/build/ivy-web.xml">
            <!ENTITY search SYSTEM "../sirius/build/ivy-search.xml">
            <!ENTITY app SYSTEM "../sirius/build/ivy-app.xml">
    ]>
    <ivy-module version="2.0">
        <info organisation="my.organization" module="myproduct"/>

        <configurations defaultconfmapping="compile->compile;compile->master;test->compile;test->master">
            <conf name="compile" />
            <conf name="test" />
        </configurations>

        <dependencies>

            <!-- Remove includes if not required -->
            &kernel;
            &web;
            &search;
            &app;

            <!-- Include custom dependencies here -->

        </dependencies>
    </ivy-module>
