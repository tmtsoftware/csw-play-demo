Command Service Play Demo
=========================

This project contains a Play/ScalaJS web application that works with the assemblies and HCDs in the
<a href="https://github.com/tmtsoftware/csw-pkg-demo">csw-pkg-demo</a> project. 

See the instructions there for how to run the backend assemblies and HCDs.
Then you can start the play web app by running `sbt run` in this directory.
Visit http://localhost:9000 to view the web app for the assembly and
change the settings.

Sbt Build
---------

The build consists of three projects: a server, client and shared (Scala and JavaScript) project.
The Play server project is set as the default.

An install.sh script is provided to build and install the demo under ../install.

Running
-------

After running install.sh, a `demowebserver` script can be found under ../install/bin for starting the web app.
To test, you can first start the csw-pkg-demo assemblies with the `test_containers.sh` command in one shwll window,
and then run `demowebserver` in another window. Then visit http://localhost:9000 in a browser.

Internals
---------

The scala.js web app makes Ajax calls to a Play server and receives status updates via websockets.
The Play server uses the command service REST API to send commands and receive replies.
(The web app can't use the command service REST API directly due to cross site security restrictions, 
since it comes from a different host and/or port.)
The local Play server is still useful for providing a simpler, custom REST API for the web app. 

Websockets
----------

A single websocket is used to update the browser with the command status and current values.
Since all connected browsers receive the same websocket messages, they are automatically kept
up to date. You can test this by opening different browsers and changing values.
The Refresh button can also be used to update the values from the assemblies.




