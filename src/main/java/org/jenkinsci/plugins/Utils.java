package org.jenkinsci.plugins;

import java.io.File;

public class Utils {
    public static boolean isEmpty(String str){
        if (str == null || str.isEmpty()){
            return true;
        }
        return false;
    }


}
