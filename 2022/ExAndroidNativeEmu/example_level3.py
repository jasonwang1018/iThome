import logging
import posixpath
from unicorn import *
from unicorn.arm64_const import *
from androidemu.java.classes.array import Array
from androidemu.emulator import Emulator
from androidemu.const import emu_const


def hook_code(uc_engine, address, size, user_data):
	if address in instructions_list:
		w10 = uc_engine.reg_read(UC_ARM64_REG_W10)
		w12 = uc_engine.reg_read(UC_ARM64_REG_W12)
		print('UnCrackable3: w10=0x%X("%s"), w12=0x%X("%s")' % (w10, chr(w10), w12, chr(w12)))
		uc_engine.reg_write(UC_ARM64_REG_W12, w10)


emulator = Emulator(vfs_root=posixpath.join(posixpath.dirname(__file__), "vfs"), arch=emu_const.ARCH_ARM64)
libfoo3_module = emulator.load_library("tests/bin64/libfoo3.so")
instructions_list = [libfoo3_module.base + 0x3450]
emulator.mu.hook_add(UC_HOOK_CODE, hook_code, emulator, libfoo3_module.base, libfoo3_module.base + libfoo3_module.size)

try:
	xorkey = b'pizzapizzapizzapizzapizz'
	emulator.call_symbol(libfoo3_module, 'Java_sg_vantagepoint_uncrackable3_MainActivity_init', emulator.java_vm.jni_env.address_ptr, 0x00, Array(bytearray(xorkey)))

	print("UnCrackable3: bar() before")
	input_secret = b'123456789012345678901234'
	result = emulator.call_symbol(libfoo3_module, 'Java_sg_vantagepoint_uncrackable3_CodeCheck_bar', emulator.java_vm.jni_env.address_ptr, 0x00, Array(bytearray(input_secret)))
	print("UnCrackable3: bar() after, return=%d" % result)
except UcError as e:
	print("UnCrackable3: Exit at %x" % emulator.mu.reg_read(UC_ARM64_REG_PC))
	raise
