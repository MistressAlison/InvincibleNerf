package InvincibleNerf.patches;

import InvincibleNerf.InvincibleNerfMod;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.powers.InvinciblePower;

public class InvinciblePatches {
    @SpirePatch2(clz = InvinciblePower.class, method = "onAttackedToChangeDamage")
    public static class NerfTime {
        static int originalDamage = 0;
        static int originalAmount = 0;
        @SpirePrefixPatch
        public static void grabDamage(InvinciblePower __instance, DamageInfo info, int damageAmount) {
            originalDamage = damageAmount;
            originalAmount = __instance.amount;
        }

        @SpirePostfixPatch
        public static int nerf(@ByRef int[] __result, InvinciblePower __instance) {
            if (originalDamage > __result[0] && InvincibleNerfMod.modEnabled) {
                int fullDamage = originalAmount;
                int reducedDamage = (int) Math.max(InvincibleNerfMod.atLeastOne ? 1 : 0, (originalDamage - originalAmount) * (1 - InvincibleNerfMod.reductionPercent/100f));
                __result[0] = fullDamage + reducedDamage;
            }
            return __result[0];
        }
    }

    @SpirePatch2(clz = InvinciblePower.class, method = "updateDescription")
    public static class DescriptionStuff {
        public static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(InvincibleNerfMod.makeID("InvincibleText"));
        public static final String[] TEXT = uiStrings.TEXT;
        @SpirePostfixPatch
        public static void changeDescription(InvinciblePower __instance) {
            if (InvincibleNerfMod.modEnabled && (InvincibleNerfMod.reductionPercent != 100 || InvincibleNerfMod.atLeastOne)) {
                if (__instance.amount > 0) {
                    if (InvincibleNerfMod.reductionPercent == 100) {
                        __instance.description = String.format(TEXT[1], __instance.amount);
                    } else {
                        __instance.description = String.format(TEXT[0], __instance.amount, InvincibleNerfMod.reductionPercent + "%");
                    }
                } else {
                    if (InvincibleNerfMod.reductionPercent == 100) {
                        __instance.description = TEXT[3];
                    } else {
                        __instance.description = String.format(TEXT[2], InvincibleNerfMod.reductionPercent + "%");
                    }
                }
                if (InvincibleNerfMod.atLeastOne && InvincibleNerfMod.reductionPercent != 100) {
                    __instance.description += TEXT[4];
                }
            }
        }
    }
}
