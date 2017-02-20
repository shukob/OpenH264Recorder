cmake_minimum_required(VERSION 3.6)
project(aac_enc)
add_library( # Sets the name of the library.


             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.

             aac_enc
             SHARED
             basic_op/basicop2.c
             basic_op/oper_32b.c
                 src/cmnMemory.c
                 src/aac_rom.c
                 src/aacenc.c
                 src/aacenc_core.c
                 src/adj_thr.c
                 src/band_nrg.c
                 src/bit_cnt.c
                 src/bitbuffer.c
                 src/bitenc.c
                 src/block_switch.c
                 src/channel_map.c
                 src/dyn_bits.c
                 src/grp_data.c
                 src/interface.c
                 src/line_pe.c
                 src/memalign.c
                 src/ms_stereo.c
                 src/pre_echo_control.c
                 src/psy_configuration.c
                 src/psy_main.c
                 src/qc_main.c
                 src/quantize.c
                 src/sf_estim.c
                 src/spreading.c
                 src/stat_bits.c
                 src/tns.c
                 src/transform.c
                 src/asm/ARMV5E/AutoCorrelation_v5.s
                 src/asm/ARMV5E/band_nrg_v5.s
                 src/asm/ARMV5E/CalcWindowEnergy_v5.s
                 src/asm/ARMV5E/PrePostMDCT_v5.s
                 src/asm/ARMV5E/R4R8First_v5.s
                 src/asm/ARMV5E/Radix4FFT_v5.s
             )
target_include_directories(aac_enc PRIVATE
                           src
                           basic_op
                           inc)
