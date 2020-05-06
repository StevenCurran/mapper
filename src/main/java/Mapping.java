import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Mapping {


    public static final int PROT_READ = 0x1; /* Page can be read. */
    public static final int PROT_WRITE = 0x2; /* Page can be written. */
    public static final int PROT_EXEC = 0x4; /* Page can be executed. */
    public static final int PROT_NONE = 0x0; /* Page can not be accessed. */

    public static final int MAP_SHARED = 0x01; /* Share changes. */
    public static final int MAP_PRIVATE	= 0x0002; /* Changes are private. */
    public static final int MAP_ANON = 0x1000; /* Changes are private. */
    public static final int MAP_SHARED_MAC = 0x0001	; /* Changes are private. */
    public static final int MAP_FIXED= 0x0010;


    public static Pointer double_map(long size) {

        int rw = PROT_READ | PROT_WRITE;

        int flag = MAP_SHARED_MAC | MAP_ANON;
        int flag2 = MAP_PRIVATE | MAP_ANON;
        Pointer double_size_map = Delegate.mmap(null, new NativeLong(size << 1), PROT_READ, flag2, -1, new NativeLong(0));


        int f = MAP_SHARED_MAC | MAP_FIXED;
        Pointer map1 = Delegate.mmap(double_size_map,  new NativeLong(size), rw, f, -1, new NativeLong(0));

        Pointer second_address = new Pointer(Pointer.nativeValue(map1) + size);

        Pointer mmap2 = Delegate.mmap(second_address, new NativeLong(size), rw, f, -1, new NativeLong(0));

        return double_size_map;

    }

    /**
     * Map the given region of the given file descriptor into memory.
     * Returns a Pointer to the newly mapped memory throws an
     * IOException on error.
     */
    public static Pointer mmap(long len, int prot, int flags, int fildes, long off)
            throws IOException {



        // we don't really have a need to change the recommended pointer.
        Pointer addr = new Pointer(0);



        Pointer anon = Delegate.mmap(null, new NativeLong(len), prot, flags, fildes, new NativeLong(off));




        Pointer result = Delegate.mmap(addr,
                new NativeLong(len),
                prot,
                flags,
                fildes,
                new NativeLong(off));

        if (Pointer.nativeValue(result) == -1) {
            throw new IOException("mmap failed: ");
        }

        return result;

    }

    static class Delegate {

        public static native Pointer mmap(Pointer addr,
                                          NativeLong len,
                                          int prot,
                                          int flags,
                                          int fildes,
                                          NativeLong off);

        static {
            Native.register(Platform.C_LIBRARY_NAME);
        }

    }

    public static void main(String[] args) throws Exception {

        Pointer pointer = double_map(4096);
        ByteBuffer byteBuffer = pointer.getByteBuffer(0, 4096 * 2);




        if (Files.exists(Paths.get("/var/tmp/steventc"))) {
            Files.delete(Paths.get("/var/tmp/steventc"));
        }
        File file = Files.createFile(Paths.get("/var/tmp/steventc")).toFile();
        Files.write(file.toPath(), "Hello world".getBytes());

        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        int fd = sun.misc.SharedSecrets.getJavaIOFileDescriptorAccess()
                .get(randomAccessFile.getFD());

        // mmap a large file...
        Pointer addr = mmap(file.length(), PROT_READ, Mapping.MAP_SHARED, fd, 0L);

        System.out.println(Pointer.nativeValue(addr));
        System.out.println(addr);


    }
}
