The SIRIUS Framework
====================

SIRIUS is a modern framework written in Java which all functionality required to build robust commercial grade applications. 
Although it is primarily being used as a platform to build web applications, it can also be beneficial for rich client
applications as *OpenCobra* shows.

OpenSource Applications using SIRIUS
------------------------------------
* S3Ninja (http://s3ninja.net)
* Software Distribution System (https://github.com/scireum/sds-server)
* OpenCobra (http://opencobra.com)

Main Concepts and Key Terms
---------------------------
SIRIUS is a modern framework written in Java which all functionality required to build robust commercial grade applications. 
Although it is primarily being used as a platform to build web applications, it can also be beneficial for rich client
applications as *OpenCobra* shows.

Kernel Profile
--------------
SIRIUS is a modern framework written in Java which all functionality required to build robust commercial grade applications. 
Although it is primarily being used as a platform to build web applications, it can also be beneficial for rich client
applications as *OpenCobra* shows.

Web Profile
-----------
SIRIUS is a modern framework written in Java which all functionality required to build robust commercial grade applications. 
Although it is primarily being used as a platform to build web applications, it can also be beneficial for rich client
applications as *OpenCobra* shows.

App Profile
-----------
SIRIUS is a modern framework written in Java which all functionality required to build robust commercial grade applications. 
Although it is primarily being used as a platform to build web applications, it can also be beneficial for rich client
applications as *OpenCobra* shows.

Search Profile
--------------
SIRIUS is a modern framework written in Java which all functionality required to build robust commercial grade applications. 
Although it is primarily being used as a platform to build web applications, it can also be beneficial for rich client
applications as *OpenCobra* shows

Test
----
::
            Invocable inv = (Invocable) engine;
            Object part = inv.getInterface(object, lookupClass);
            if (part == null) {
                throw Exceptions.handle()
                        .withSystemErrorMessage("Cannot convert JavaScript-Object '%s' into '%s'!",
                                object,
                                lookupClass)
                        .to(LOG)
                        .handle();
            }
