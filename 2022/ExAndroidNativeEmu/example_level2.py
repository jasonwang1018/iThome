import logging
import posixpath
import sys
from unicorn import *
from unicorn.arm_const import *
from androidemu.java.classes.array import Array
from androidemu.emulator import Emulator
from androidemu.native_hook_utils import FuncHooker
from androidemu.const import emu_const
from androidemu.utils import memory_helpers
from androidemu.utils.chain_log import ChainLogger
import androidemu.utils.debug_utils
from tests.test_thread import TestThread

instructions_skip_list = []
g_cfd = ChainLogger(sys.stdout, "./ins-level2.txt")


def hook_code(mu, address, size, user_data):
	# print('>>> Tracing instruction at 0x%x, instruction size = 0x%x' % (address, size))
	try:
		emu = user_data
		if (not emu.memory.check_addr(address, UC_PROT_EXEC)):
			logger.error("addr 0x%08X out of range" % (address,))
			sys.exit(-1)
		# androidemu.utils.debug_utils.dump_registers(mu, sys.stdout)
		androidemu.utils.debug_utils.dump_code(emu, address, size, g_cfd)
	except Exception as e:
		logger.exception("exception in hook_code")
		sys.exit(-1)
	if address in instructions_skip_list:
		mu.reg_write(UC_ARM_REG_PC, address + size + 1)


def strncmp_before(emu, *arg):
	s1 = memory_helpers.read_utf8(emu.mu, arg[0])
	s2 = memory_helpers.read_utf8(emu.mu, arg[1])
	print('UnCrackable2: strncmp() before, s1="%s", s2="%s", n=%d' % (s1, s2, arg[2]))
	return False


def strncmp_after(emu, x0, x1):
	print('UnCrackable2: strncmp() after, return=0x%08X' % x0)
	return False


logger = logging.getLogger(__name__)

emulator = Emulator(vfs_root=posixpath.join(posixpath.dirname(__file__), "vfs"), arch=emu_const.ARCH_ARM64)
# emulator.mu.hook_add(UC_HOOK_CODE, hook_code, emulator)

lib_module = emulator.load_library("tests/bin64/libfoo2.so")
# androidemu.utils.debug_utils.dump_symbols(emulator, sys.stdout)

strncmp_symbol = 0
for module in emulator.modules:
	# logger.info("level2: module_name=[%s], module_base=0x%08x, module_size=0x%08x" % (module.filename, module.base, module.size))
	if 'libc.so' in module.filename:
		strncmp_symbol = module.find_symbol('strncmp')

try:
	function_hooker = FuncHooker(emulator)
	function_hooker.fun_hook(strncmp_symbol, 3, strncmp_before, strncmp_after)

	emulator.call_symbol(lib_module, 'Java_sg_vantagepoint_uncrackable2_MainActivity_init', emulator.java_vm.jni_env.address_ptr, 0x00)

	print("UnCrackable2: bar() before")
	input_secret_string = b'12345678901234567890123'
	java_byte_array = Array(bytearray(input_secret_string))
	result = emulator.call_symbol(lib_module, 'Java_sg_vantagepoint_uncrackable2_CodeCheck_bar', emulator.java_vm.jni_env.address_ptr, 0x00, java_byte_array)
	print("UnCrackable2: bar() after, return=%d\n" % result)
except UcError as e:
	print("UnCrackable2: Exit at %x" % emulator.mu.reg_read(UC_ARM_REG_PC))
	raise
