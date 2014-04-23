package net.fusejna.nio

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.fusejna.DirectoryFiller
import net.fusejna.ErrorCodes
import net.fusejna.FuseException
import net.fusejna.StructFuseFileInfo
import net.fusejna.StructStat
import net.fusejna.types.TypeMode
import net.fusejna.util.FuseFilesystemAdapterAssumeImplemented
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class NioFS extends FuseFilesystemAdapterAssumeImplemented {

    private Path rootDirectory

    private boolean isPosix

    NioFS( Path targetDir ) {
        rootDirectory = targetDir
    }

    public static void main(final String... args) throws FuseException {
        if (args.length != 2) {
            System.err.println("Usage: NioFS <mount-point> <target-path>");
            System.exit(1);
        }

        def target = Paths.get(args[1])
        if( !Files.exists(target) )
            throw new IllegalArgumentException("Target path to not exists: $target")

        new NioFS(target).mount(args[0]);
    }

    private Path target( String path ) { rootDirectory.resolve(path) }

    /**
     * Check file access permissions
     * <p>
     * This will be called for the access() system call. If the 'default_permissions' mount option is given, this method is not called.
     *
     * @param path
     * @param access
     * @return
     */
    @Override
    int access(final String path, final int access) {
        return 0;
    }

    /**
     * Create and open a file. If the file does not exist, first create it with the specified mode, and then open it.
     *
     * @param path
     * @param mode
     * @param info
     * @return
     */
    @Override
    public int create(final String path, final TypeMode.ModeWrapper mode, final StructFuseFileInfo.FileInfoWrapper info)
    {
        def target = target(path)
        if( Files.exists(target) ) {
            return -ErrorCodes.EEXIST();
        }

        try {
            Files.createFile(target)
            return 0
        }
        catch( Throwable e ) {
            log.error("Unable to create file: $path", e)
            // TODO return a specific error code
        }

        return -ErrorCodes.ENOENT();
    }

    /**
     * Get file attributes
     *
     * @param path
     * @param stat
     * @return
     */
    @Override
    int getattr(final String path, final StructStat.StatWrapper stat) {

        final target = rootDirectory.resolve(path)
        if( Files.exists(target) ) {
            def attr = readAttr(target)
            stat.with {
                size( attr.size() )
                ctime( attr.creationTime().toMillis() )
                atime( attr.lastAccessTime().toMillis() )
                mtime( attr.lastModifiedTime().toMillis() )

                TypeMode.NodeType type
                if( attr.isDirectory() ) {
                    type = TypeMode.NodeType.DIRECTORY
                }
                else if( attr.isSymbolicLink() ) {
                    type = TypeMode.NodeType.SYMBOLIC_LINK
                }
                else {
                    type = TypeMode.NodeType.FILE
                }

                if( attr instanceof PosixFileAttributes ) {
                    def perm = (attr as PosixFileAttributes).permissions()
                    setMode(type,
                            PosixFilePermission.OWNER_READ in perm,
                            PosixFilePermission.OWNER_WRITE in perm,
                            PosixFilePermission.OWNER_EXECUTE in perm,
                            PosixFilePermission.GROUP_READ in perm,
                            PosixFilePermission.GROUP_WRITE in perm,
                            PosixFilePermission.GROUP_EXECUTE in perm,
                            PosixFilePermission.OTHERS_READ in perm,
                            PosixFilePermission.OTHERS_WRITE in perm,
                            PosixFilePermission.OTHERS_EXECUTE in perm )
                }
                else {
                    setMode(type)
                }
            }

            return 0
        }

        return -ErrorCodes.ENOENT();
    }

    def BasicFileAttributes readAttr( Path path ) {
        if( isPosix ) {
            return Files.readAttributes(path, PosixFileAttributes)
        }
        else {
            return Files.readAttributes(path,BasicFileAttributes)
        }
    }


    @Override
    public int mkdir(final String path, final TypeMode.ModeWrapper mode)
    {

        def t = target(path)
        if( Files.exists(t) ) {
            return -ErrorCodes.EEXIST();
        }

        try {
            Files.createDirectory(t)
            return 0
        }
        catch( IOException e ) {
            log.error "Unable to create directory: $t"
            return -ErrorCodes.ENOENT();
        }
    }

    @Override
    public int open(final String path, final StructFuseFileInfo.FileInfoWrapper info)
    {
        return 0;
    }

    @Override
    public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final StructFuseFileInfo.FileInfoWrapper info)
    {
        final t = target(path)
        if( !Files.exists(t) )
            return -ErrorCodes.ENOENT();

//        if (!(p instanceof MemoryFile)) {
//            return -ErrorCodes.EISDIR();
//        }

        Files.new
        return ((MemoryFile) p).read(buffer, size, offset);
    }

    @Override
    public int readdir(final String path, final DirectoryFiller filler)
    {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryDirectory)) {
            return -ErrorCodes.ENOTDIR();
        }
        ((MemoryDirectory) p).read(filler);
        return 0;
    }

    @Override
    public int rename(final String path, final String newName)
    {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        final MemoryPath newParent = getParentPath(newName);
        if (newParent == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(newParent instanceof MemoryDirectory)) {
            return -ErrorCodes.ENOTDIR();
        }
        p.delete();
        p.rename(newName.substring(newName.lastIndexOf("/")));
        ((MemoryDirectory) newParent).add(p);
        return 0;
    }

    @Override
    public int rmdir(final String path)
    {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryDirectory)) {
            return -ErrorCodes.ENOTDIR();
        }
        p.delete();
        return 0;
    }

    @Override
    public int truncate(final String path, final long offset)
    {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryFile)) {
            return -ErrorCodes.EISDIR();
        }
        ((MemoryFile) p).truncate(offset);
        return 0;
    }

    @Override
    public int unlink(final String path)
    {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        p.delete();
        return 0;
    }

    @Override
    public int write(final String path, final ByteBuffer buf, final long bufSize, final long writeOffset,
                     final StructFuseFileInfo.FileInfoWrapper wrapper)
    {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryFile)) {
            return -ErrorCodes.EISDIR();
        }
        return ((MemoryFile) p).write(buf, bufSize, writeOffset);
    }


}
