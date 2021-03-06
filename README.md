[![Build Status](https://travis-ci.org/EtiennePerot/fuse-jna.png)](https://travis-ci.org/EtiennePerot/fuse-jna)

## The straight dope
#### Grab it

    git clone git://github.com/EtiennePerot/fuse-jna.git

#### Run it

    fuse-jna/examples/hellofs.sh ~/hellofs

You now have a test filesystem in `~/hellofs`.
You'll find a file called `hello.txt` in there.

There's another example filesystem that you can use, which is writable:

    fuse-jna/examples/memoryfs.sh ~/memoryfs

#### Make your own filesystem

1. Subclass `net.fusejna.FuseFilesystem` and override the methods you need (For convenience, there is an adapter called `FuseFilesystemAdapterFull`).

2. Create an instance of your subclass, then call `.mount(mountpoint)` on it.

3. The filesystem will be unmounted automatically at JVM shutdown time if possible. You can unmount it at runtime using `.unmount()`.

4. ???

5. Profit.

6. *(Bonus)* You can get logging for free by calling `.log(true)` or `.log(myLogger)` on the filesystem instance.

## The longer stuff
#### The idea
fuse-jna was born out of the desire for no-compilation-required, no-bullshit, actually-working bindings to the FUSE library.

Originally built to make the OS X/Linux port of [SrcDemo²] possible, it was separated into its own library because I figured others would benefit from it.

I like Python, and I like using [fuse.py] when writing FUSE filesystems in Python. Thanks to `ctypes`, it comes in just one file and that's all you need for nice 'n' straight Python bindings.
The goal of fuse-jna is to bring FUSE bindings to Java with the same simplicity.

To do that, it uses [JNA], which itself was inspired by Python's `ctypes` in terms of ease-of-use.

#### Compatibility

Following [fuse.py], fuse-jna should work with:

* OS X with [MacFUSE]/[fuse4x]/[OSXFUSE] on Intel architectures
* Linux with [FUSE][Linux-Fuse] on Intel and PowerPC architectures
* FreeBSD with [FUSE][FreeBSD-Fuse] on Intel architectures

#### Projects using fuse-jna

* [JGitFS](https://github.com/centic9/JGitFS): Displays Git branches, tags and commits as files
* [GithubFS](https://github.com/akiellor/githubfs): Expose GitHub issues as files

Feel free to [open an issue](https://github.com/EtiennePerot/fuse-jna/issues/new) to get your project added here.

#### Licensing
fuse-jna is licensed under the [BSD 2-Clause License].
JNA is licensed under the [LGPL v2.1].

[SrcDemo²]: https://github.com/EtiennePerot/srcdemo2
[fuse.py]: http://code.google.com/p/fusepy/source/browse/trunk/fuse.py
[JNA]: https://github.com/twall/jna
[MacFUSE]: http://code.google.com/p/macfuse/
[fuse4x]: http://fuse4x.org/
[OSXFUSE]: http://osxfuse.github.com/
[Linux-FUSE]: http://fuse.sourceforge.net/
[FreeBSD-FUSE]: http://wiki.freebsd.org/FuseFilesystem
[BSD 2-Clause License]: http://www.opensource.org/licenses/bsd-license.php
[LGPL v2.1]: http://www.opensource.org/licenses/lgpl-2.1.php
