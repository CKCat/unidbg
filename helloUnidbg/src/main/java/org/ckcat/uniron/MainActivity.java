package org.ckcat.uniron;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyClassFactory;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.net.URL;
import java.util.Base64;

public class MainActivity {
    public String objcontent = "objcontent";
    public static String staticcontent = "staticcontent";

    public String base64(String content){
        return Base64.getEncoder().encodeToString(content.getBytes());
    }
    public static void main(String[] args) {
        URL resourceUrl = Main.class.getClassLoader().getResource("libuniron.so");
        if (resourceUrl == null) {
            return;
        }
        String filePath = resourceUrl.getFile();
        System.out.println("文件路径: " + filePath);
        // 1. 创建 emulator
        AndroidEmulator emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName("org.ckcat.uniron")
                .build();
        // 2. 设置依赖库
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        // 3. 创建vm
        VM vm = emulator.createDalvikVM();
        vm.setVerbose(true); // 输出日志
        // 使用代理方式来处理所有 Java 类的加载
        vm.setDvmClassFactory(new ProxyClassFactory());
        DalvikModule dm = vm.loadLibrary(new File(filePath),true);
        dm.callJNI_OnLoad(emulator);
        MainActivity mainActivity = new MainActivity();
        // 创建一个适配于 Unidbg 环境的代理对象，用于模拟 Android 中的 DvmObject 行为，Unidbg 在执行 JNI 调用时，
        // 会通过反射机制动态解析并绑定 Java 对象中的字段（如 objcontent、staticcontent）和方法（如 base64），实现对 Dalvik VM 的模拟
        DvmObject<?> obj = ProxyDvmObject.createObject(vm, mainActivity);
        obj.callJniMethodObject(emulator, "base64byjni(Ljava/lang/String;)Ljava/lang/String;","callbase64byjni");

    }
}
