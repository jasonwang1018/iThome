package owasp.mstg.uncrackable3;

import com.github.unidbg.*;
import com.github.unidbg.arm.backend.HypervisorFactory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyClassFactory;
import com.github.unidbg.memory.Memory;
import com.sun.jna.Pointer;
import unicorn.Arm64Const;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class fooArm64
{
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    private fooArm64()
    {
        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName("owasp.mstg.uncrackable3").addBackendFactory(new HypervisorFactory(true)).build();
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM();
        vm.setDvmClassFactory(new ProxyClassFactory());
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/arm64-v8a/libfoo3.so"), false);
        module = dm.getModule();
    }

    private void runEmulator()
    {
        emulator.attach().addBreakPoint(module, 0x3450, new BreakPointCallback()
        {
            @Override
            public boolean onHit(Emulator<?> emulator, long address)
            {
                RegisterContext context = emulator.getContext();
                int w10 = context.getIntByReg(Arm64Const.UC_ARM64_REG_W10);
                int w12 = context.getIntByReg(Arm64Const.UC_ARM64_REG_W12);
                System.out.printf("UnCrackable3: w10=0x%X(\"%s\"), w12=0x%X(\"%s\")\n", w10, (char) w10, w12, (char) w12);
                emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_W12, w10);
                return true;
            }
        });

        Pointer jniEnv = vm.getJNIEnv();
        DvmObject<?> thiz = vm.resolveClass("owasp.mstg.uncrackable3.MainActivity").newObject(null);
        List<Object> args = new ArrayList<>();
        args.add(jniEnv);
        args.add(vm.addLocalObject(thiz));
        String xorkey = "pizzapizzapizzapizzapizz";
        args.add(vm.addLocalObject(new ByteArray(vm, xorkey.getBytes())));
        module.callFunction(emulator, "Java_sg_vantagepoint_uncrackable3_MainActivity_init", args.toArray());

        System.out.println("UnCrackable3: bar() before");
        thiz = vm.resolveClass("owasp.mstg.uncrackable3.CodeCheck").newObject(null);
        args = new ArrayList<>();
        args.add(jniEnv);
        args.add(vm.addLocalObject(thiz));
        String inputSecret = "123456789012345678901234";
        args.add(vm.addLocalObject(new ByteArray(vm, inputSecret.getBytes())));
        int result = module.callFunction(emulator, "Java_sg_vantagepoint_uncrackable3_CodeCheck_bar", args.toArray())[0].intValue();
        System.out.printf("UnCrackable3: bar() after, return=%d\n", result);
    }

    public static void main(String[] args) throws Exception
    {
        fooArm64 foo3 = new fooArm64();
        foo3.runEmulator();
        foo3.emulator.close();
    }
}
