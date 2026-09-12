// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from Dwinovo/minecraft-numen 34ef004dac3095fbbd928a897927e277c69d02fa.
package dev.mcai.companion.vendor.numen.memory;

import java.util.List;
import com.google.gson.JsonObject;

/** Numen's recent-history budget/safe split, using server-owned message DTOs. */
public final class CompactSplit {
    private CompactSplit() {}
    public record Split(List<JsonObject> toSummarize,List<JsonObject> kept) {}
    public static Split byRecentBudget(List<JsonObject> history,int budgetTokens){
        int cutUser=-1,cutAssistant=-1;long acc=0;
        for(int i=history.size()-1;i>=0;i--){var message=history.get(i);acc+=estimateTokens(message.get("text").getAsString());if(acc>budgetTokens)break;
            if(message.get("role").getAsString().equals("user"))cutUser=i;else if(message.get("role").getAsString().equals("assistant"))cutAssistant=i;
        }
        int cut=cutUser>=0?cutUser:cutAssistant>=0?cutAssistant:history.size();
        return new Split(List.copyOf(history.subList(0,cut)),List.copyOf(history.subList(cut,history.size())));
    }
    public static int estimateTokens(String text){long cjk=0,ascii=0;if(text!=null)for(int i=0;i<text.length();i++){if(text.charAt(i)>0x2E7F)cjk++;else ascii++;}return (int)Math.min(Integer.MAX_VALUE,cjk+ascii/4+8);}
}
