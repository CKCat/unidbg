package org.ckcat.uniron;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.PointerNumber;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import org.apache.commons.codec.binary.Base64;
import unicorn.Arm64Const;

import java.io.File;
import java.net.URL;

public class Main extends AbstractJni {


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
        // 4. 加载 so, 这里会自动调用 .init_proc 和 init_array 里面的函数。
        DalvikModule dm = vm.loadLibrary(new File(filePath), false);
        // 5. 调用普通函数
        // 通过偏移调用函数
        Number result= dm.getModule().callFunction(emulator,  0xED0, 11,22);
        System.out.println(result.intValue());
        // 写入字符串参数
        MemoryBlock block = memory.malloc(10, false);
        UnidbgPointer pointer = block.getPointer();
        pointer.write("flag".getBytes());

        result= dm.getModule().callFunction(emulator,  0xEF0, new PointerNumber(pointer),1, 2, 3, 4, 5, 6,7,8,9,10);
        Number x0 = emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_X0);
        System.out.println(result.intValue() + " --- " + x0);

        // 6. 调用JNI函数
        System.out.println("================start callStaticJniMethodObject sayWorld================================");
        DvmClass main_activity = vm.resolveClass("org/ckcat/uniron/MainActivity");
        DvmObject<?> resultObj = main_activity.callStaticJniMethodObject(emulator, "sayWorld(Ljava/lang/String;)Ljava/lang/String;",
                new StringObject(vm, "Say World!!!"));
        System.out.println(resultObj.toString());
        System.out.println("================end callStaticJniMethodObject sayWorld================================");
        // 7. 调用 JNI_OnLoad 函数
        dm.callJNI_OnLoad(emulator);
        // 对于动态注册的jni函数必须在完成地址的绑定才能调用
        Main main = new Main();
        vm.setJni(main);
        System.out.println("================start callStaticJniMethodObject sayHello================================");
        resultObj = main_activity.callStaticJniMethodObject(emulator, "sayHello(Ljava/lang/String;)Ljava/lang/String;",
                new StringObject(vm, "Say Hello!!!"));
        System.out.println(resultObj.toString());
        System.out.println("================end callStaticJniMethodObject sayHello================================");

        System.out.println("================start callJniMethodObject base64byjni================================");
        DvmObject<?> main_activityobj = main_activity.newObject(emulator);
        resultObj = main_activityobj.callJniMethodObject(emulator, "base64byjni(Ljava/lang/String;)Ljava/lang/String;", "base46jni_call");
        System.out.println(resultObj.toString());
        System.out.println("================end callJniMethodObject base64byjni================================");

    }
    public String base64(String content) {
        return Base64.encodeBase64String(content.getBytes());
    }
    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, DvmMethod dvmMethod, VaList vaList) {
        System.out.println("callStaticObjectMethodV");
        System.out.println(dvmClass.toString());
        System.out.println(dvmMethod.toString());
        String signature = dvmMethod.getSignature();
        if(signature.equals("org/ckcat/uniron/Encrypt->base64(Ljava/lang/String;)Ljava/lang/String;")){
            DvmObject<?> dvmobj=vaList.getObjectArg(0);
            String arg= (String) dvmobj.getValue();
            String result=base64(arg);
            return new StringObject(vm,result);
        }
        return super.callStaticObjectMethodV(vm, dvmClass, dvmMethod, vaList);
    }
    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        if (signature.equals("org/ckcat/uniron/MainActivity->staticcontent:Ljava/lang/String;")) {
            return new StringObject(vm, "CKCat-staticObject");
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        if (signature.equals("org/ckcat/uniron/MainActivity->objcontent:Ljava/lang/String;")) {
            return new StringObject(vm, "CKCat-Object");
        }
        return super.getObjectField(vm, dvmObject, signature);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if(signature.equals("org/ckcat/uniron/MainActivity->base64(Ljava/lang/String;)Ljava/lang/String;")){
            DvmObject<?> dvmobj=vaList.getObjectArg(0);
            String arg= (String) dvmobj.getValue();
            String result=base64(arg);
            return new StringObject(vm,result);
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }
}