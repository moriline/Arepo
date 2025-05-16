package org.arepo;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;

import java.util.Arrays;
import java.util.List;

public class Undiff {
    public static void main(String[] args) throws PatchFailedException {
        one();
        //two();
        //three();
    }
    public static void three(){
        List<String> text1=List.of();
        List<String> text2=Arrays.asList("aa","bb", "cc");
        //generating diff information.
        Patch<String> diff = DiffUtils.diff(text1, text2);
        //generating unified diff format
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("original-file.txt", "new-file.txt", text1, diff, 0);

        unifiedDiff.forEach(System.out::println);
        var patch = UnifiedDiffUtils.parseUnifiedDiff(unifiedDiff);
        var rev2 = DiffUtils.unpatch(text2, patch);
        System.out.println("unpatch rev:"+rev2);
    }
    public static void two() throws PatchFailedException {
        List<String> text1=Arrays.asList("this is a test","a test");
        List<String> text2=Arrays.asList("this is a testfile","a test");

        //generating diff information.
        Patch<String> diff = DiffUtils.diff(text1, text2);

        //generating unified diff format
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("original-file.txt", "new-file.txt", text1, diff, 0);

        unifiedDiff.forEach(System.out::println);
        var patch = UnifiedDiffUtils.parseUnifiedDiff(unifiedDiff);
        var rev = DiffUtils.patch(text1, patch);
        System.out.println("patch rev:"+rev);
        var revunpatch = DiffUtils.unpatch(text2, patch);
        System.out.println("unpatch rev2:"+revunpatch);
    }
    public static void one() throws PatchFailedException {
        var three = """
                aa
                xx
                zz
                """;
        var second = """
                aa
                bb
                cc
                """;
        var first = "";
        //generating diff information.
        var diffExp = DiffUtils.diff(first, second, null);

        //generating unified diff format
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("original-file.txt", "new-file.txt", List.of(), diffExp, 0);
        unifiedDiff.forEach(System.out::println);
        var patch = UnifiedDiffUtils.parseUnifiedDiff(unifiedDiff);
        var rev2 = DiffUtils.unpatch(Arrays.asList(second.split("\n")), patch);
        System.out.println("unpatch rev:"+rev2);
        /*
        var patch = DiffUtils.diffInline(second, first);
        System.out.println("p 1:"+patch);
        var patch2 = DiffUtils.diffInline(three, second);
        System.out.println("p 2:"+patch2);
        //var rev2 = DiffUtils.patch(Arrays.asList(three.split("\n")), patch2);
        var tmp = Arrays.asList(three.split("\n"));
        var rev2 = DiffUtils.unpatch(tmp, patch2);
        System.out.println("rev 2:"+rev2);

         */
    }
}
