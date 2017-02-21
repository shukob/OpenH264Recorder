cmake_minimum_required(VERSION 3.6)
project(libyuv)


add_library( # Sets the name of the library.


             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.

             libyuv
             SHARED
    source/compare.cc           
    source/compare_common.cc    
    source/compare_neon64.cc    
    source/compare_gcc.cc       
    source/convert.cc           
    source/convert_argb.cc      
    source/convert_from.cc      
    source/convert_from_argb.cc 
    source/convert_to_argb.cc   
    source/convert_to_i420.cc   
    source/cpu_id.cc            
    source/planar_functions.cc  
    source/rotate.cc            
    source/rotate_any.cc        
    source/rotate_argb.cc       
    source/rotate_common.cc     
    source/rotate_dspr2.cc      
    source/rotate_neon64.cc     
    source/rotate_gcc.cc        
    source/row_any.cc           
    source/row_common.cc        
    source/row_dspr2.cc         
    source/row_neon64.cc        
    source/row_gcc.cc           
    source/scale.cc             
    source/scale_any.cc         
    source/scale_argb.cc        
    source/scale_common.cc      
    source/scale_dspr2.cc       
    source/scale_neon64.cc      
    source/scale_gcc.cc         
    source/video_common.cc
             )

set_target_properties(libyuv PROPERTIES LINKER_LANGUAGE CXX)
target_include_directories(libyuv PRIVATE
                           include)
