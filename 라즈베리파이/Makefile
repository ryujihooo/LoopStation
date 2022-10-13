all:Insmod Mknod

Insmod:
	insmod fpga_interface_driver.ko
	insmod fpga_led_driver.ko
	insmod fpga_push_switch_driver.ko
	insmod fpga_step_motor_driver.ko

Mknod:
	mknod /dev/fpga_led c 260 0
	mknod /dev/fpga_push_switch c 260 0
	mknod /dev/fpga_step_motor c 267 0

clean:
	rm /dev/fpga_*
	rmmod fpga_led_driver.ko
	rmmod fpga_push_switch_driver.ko
	rmmod fpga_step_motor_driver.ko
	rmmod fpga_interface_driver.ko
