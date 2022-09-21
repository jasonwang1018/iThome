package owasp.mstg.uncrackable2;

import com.github.unidbg.*;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.backend.HypervisorFactory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.whale.IWhale;
import com.github.unidbg.hook.whale.Whale;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.XHookImpl;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyClassFactory;
import com.github.unidbg.memory.Memory;
import com.sun.jna.Pointer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class fooArm64
{
    private final AndroidEmulator emulator;
    private final Module module;

    private fooArm64()
    {
        //Logger.getLogger(AbstractEmulator.class).setLevel(Level.DEBUG);
        //emulator.traceCode();
        emulator = createARMEmulator();
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(createLibraryResolver());
        VM vm = emulator.createDalvikVM();
        vm.setDvmClassFactory(new ProxyClassFactory());
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/arm64-v8a/libfoo2.so"), false);
        module = dm.getModule();

        /*IxHook xHook = XHookImpl.getInstance(emulator);
        xHook.register("libfoo.so", "strncmp", new ReplaceCallback()
        {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction)
            {
                String s1 = context.getPointerArg(0).getString(0);
                String s2 = context.getPointerArg(1).getString(0);
                int n = context.getIntArg(2);
                System.out.printf("my_strncmp onCall: s1=[%s], s2=[%s], n=%d\n", s1, s2, n);
                return HookStatus.RET(emulator, originFunction);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context)
            {
                System.out.printf("my_strncmp postCall: return=0x%08x\n", context.getIntArg(0));
            }
        });
        xHook.refresh();*/

        /*IWhale whale = Whale.getInstance(emulator);
        Symbol symbol = emulator.getMemory().findModule("libc.so").findSymbolByName("strncmp");
        whale.inlineHookFunction(symbol, new ReplaceCallback()
        {
            @Override
            public HookStatus onCall(Emulator<?> emulator, long originFunction)
            {
                HookContext context = emulator.getContext();
                String s1 = context.getPointerArg(0).getString(0);
                String s2 = context.getPointerArg(1).getString(0);
                int n = context.getIntArg(2);
                System.out.printf("my_strncmp onCall: s1=[%s], s2=[%s], n=%d\n", s1, s2, n);
                return HookStatus.RET(emulator, originFunction);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context)
            {
                System.out.printf("my_strncmp postCall: return=0x%08x\n", context.getIntArg(0));
            }
        });*/

        //long address = emulator.getMemory().findModule("libc.so").findSymbolByName("strncmp").getAddress();

        emulator.attach().addBreakPoint(module, 0x820, new BreakPointCallback()
        {
            @Override
            public boolean onHit(Emulator<?> emulator, long address)
            {
                RegisterContext context = emulator.getContext();
                String s1 = context.getPointerArg(0).getString(0);
                String s2 = context.getPointerArg(1).getString(0);
                int n = context.getIntArg(2);
                System.out.printf("UnCrackable2: strncmp() before, s1=\"%s\", s2=\"%s\", n=%d\n", s1, s2, n);
                emulator.attach().addBreakPoint(context.getLRPointer().peer, new BreakPointCallback()
                {
                    @Override
                    public boolean onHit(Emulator<?> emulator, long address)
                    {
                        RegisterContext context = emulator.getContext();
                        System.out.printf("UnCrackable2: strncmp() after, return=0x%08x\n", context.getIntArg(0));
                        return true;
                    }
                });
                return true;
            }
        });

        Pointer jniEnv = vm.getJNIEnv();
        DvmObject<?> thiz = vm.resolveClass("owasp.mstg.uncrackable2.MainActivity").newObject(null);
        List<Object> args = new ArrayList<>();
        args.add(jniEnv);
        args.add(vm.addLocalObject(thiz));
        module.callFunction(emulator, "Java_sg_vantagepoint_uncrackable2_MainActivity_init", args.toArray());

        System.out.println("UnCrackable2: bar() before");
        thiz = vm.resolveClass("owasp.mstg.uncrackable2.CodeCheck").newObject(null);
        args = new ArrayList<>();
        args.add(jniEnv);
        args.add(vm.addLocalObject(thiz));
        String inputSecretString = "12345678901234567890123";
        ByteArray byteArray = new ByteArray(vm, inputSecretString.getBytes());
        args.add(vm.addLocalObject(byteArray));
        int result = module.callFunction(emulator, "Java_sg_vantagepoint_uncrackable2_CodeCheck_bar", args.toArray())[0].intValue();
        System.out.println("UnCrackable2: bar() after, return=" + result);
    }

    public static void main(String[] args) throws Exception
    {
        fooArm64 test = new fooArm64();
        test.destroy();
    }

    private static LibraryResolver createLibraryResolver()
    {
        return new AndroidResolver(23);
    }

    private static AndroidEmulator createARMEmulator()
    {
        return AndroidEmulatorBuilder.for64Bit()
                                     .setProcessName("owasp.mstg.uncrackable2")
                                     .addBackendFactory(new HypervisorFactory(true))
                                     .build();
    }

    private void destroy() throws IOException
    {
        emulator.close();
        //System.out.println("destroy");
    }
}
