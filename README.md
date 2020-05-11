

# Introduction
It recently caught my attention that I missed a funny, though not really serious source code leak at https://opensource.apple.com/. What bothered me here was that I had several automations running that do detect similar events for other repositories, but my Apple opensource automation was not running for years. So, I decided to replace my legacy shit bot with a new shit bot and there we go.

What gath can do for you is very simple. Gath observes a configurable set of tarballs for changes by frequently downloading those archives and hashing them. Once a difference is detected, the archive is stored in a folder and tagged by name, version and hash. That's all, nothing fancy here.

Do I believe that you will identify epic source code leaks from the depth of Apples secret coding prisions? I doubt it, but dude...Who have ever thought that a legitimately elected President of US and A would recommend injecting bleach as a cure for a virus infection!? What's the probability for that?

You see, sometimes it can be worth to count on the unexpected.

Long live Botswana!

# Requirements

Just to piss off people gath was implemented in Java applying absolutely not coding convention. I hope you appreciate this effort. JRE required is 11+. If you don't know what that is, go straight to hell!

# Usage

Compile/Download the gath jar and put it in a directory in which the executing user does have RWX, to be able to create files and directories.

For each detection, gath is creating a distinct folder with the package inside identified by tarball name and version as well as a hash of the package.

Be aware that, depending on the polling interval you might accumulate quite some traffic over the course of runtime as tarballs need to be downloaded to compare their hashes. Either ensure that you don't have a traffic limit or choose your tarball/"poll interval" wisely.

Once you have started gath using the following command:

```console
java -jar gath.jar
```

, the bot is running infinite until you stop it.

Default configuration values are:

"root"
: "https://opensource.apple.com" ## reserved, do not change.

"interval"
: 5 ## specifies the polling interval in minutes

"products"
: "https://opensource.apple.com/release/macos-10152.html" ## the product you want to monitor (can be one or more).

"ignore-components"
: false ## if true, components section will be ignored and all components are monitored.

"components"
: n/a ## default is that all components of a product are monitored. 

The gath console does not support commandline parameters instead a file is required for customization. Find more information about it in the configuration section.

To understand how products and component names are assembled have a look at following tarball link:

https://opensource.apple.com/tarballs/xnu/xnu-6153.61.1.tar.gz

The link contains all the values you need to create the product entry, as well as a matching component entry. You find these links on https://opensource.apple.com/.

# Configuration

You most likely want to create your own config.json file. For that purpose you can use the sample configuration from this repository which you can see below:

```json
{
"root": "https://opensource.apple.com",
"interval": 6,
"products":[ 
"https://opensource.apple.com/release/macos-10152.html", 
"https://opensource.apple.com/release/macos-10151.html" 
],
"ignore-components":false,
"components":[
{"macos-10152": [
"xnu",
"IOFireWireAVC"
]},
{"macos-10151": [
"IOKitUser",
"IOFireWireIP"
]}
]
}
```

What the sample configuration does is:

* poll interval set to 6 minutes
* source code tarballs of macos-10152 and macos-10151 in specified order are monitored
* ignore-components is set to false, which is why the following white-list is enabled
* for each version there are two white-list entries, e.g. for macos-10152 this is the xnu kernel and the IOFireWireAVC kernel extension. Likewise for macos-10151...

# Results

On the first run, gath will download and tag whatever you have specified in your configuration (or if you haven't, all) and then start observing tarball packages. Result can be found in the "repository/" directory relative to the location your gath jar file is located.