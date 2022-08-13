package InvincibleNerf.patches;

import InvincibleNerf.InvincibleNerfMod;
import com.evacipated.cardcrawl.mod.stslib.damagemods.DamageModifierManager;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.powers.InvinciblePower;
import javassist.*;

public class InvinciblePatches {
    @SpirePatch(clz = AbstractCard.class, method = SpirePatch.CLASS)
    public static class PreModifiedField {
        public static SpireField<Boolean> preModified = new SpireField<>(() -> false);
    }

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
        public static int nerf(@ByRef int[] __result, InvinciblePower __instance, DamageInfo info) {
            if (originalDamage > __result[0] && InvincibleNerfMod.modEnabled) {
                if (cardPreModified(DamageModifierManager.getInstigator(info))) {
                    PreModifiedField.preModified.set(DamageModifierManager.getInstigator(info), false);
                    __result[0] = originalDamage;
                } else {
                    __result[0] = (int) getReducedDamage(originalAmount, originalDamage);
                }
            }
            return __result[0];
        }
    }

    public static boolean cardPreModified(Object o) {
        if (o instanceof AbstractCard) {
            return PreModifiedField.preModified.get(o);
        }
        return false;
    }

    public static float getReducedDamage(int stacks, float damage) {
        return stacks + Math.max(InvincibleNerfMod.atLeastOne ? 1 : 0, (damage - stacks) * (1 - InvincibleNerfMod.reductionPercent/100f));
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

    @SpirePatch2(clz = InvinciblePower.class, method = SpirePatch.CONSTRUCTOR)
    public static class AddAtDamageReceive {
        @SpireRawPatch
        public static void addMethod(CtBehavior ctMethodToPatch) throws NotFoundException, CannotCompileException {
            CtClass ctClass = ctMethodToPatch.getDeclaringClass();
            ClassPool classPool = ctClass.getClassPool();
            CtClass damageTypeClass = classPool.get(DamageInfo.DamageType.class.getName());
            CtClass abstractCardClass = classPool.get(AbstractCard.class.getName());
            CtMethod method2 = CtNewMethod.make(CtClass.floatType, "atDamageFinalReceive", new CtClass[]{CtPrimitiveType.floatType, damageTypeClass, abstractCardClass},null,
                    "{" +
                            "return "+InvinciblePatches.class.getName()+".calcDamage(amount, $1, $3);" +
                            "}",
                    ctClass);
            ctClass.addMethod(method2);
        }
    }

    public static float calcDamage(int stacks, float damage, AbstractCard card) {
        if (InvincibleNerfMod.modEnabled) {
            if (damage > stacks) {
                PreModifiedField.preModified.set(card, true);
                return getReducedDamage(stacks, damage);
            }
        }
        return damage;
    }
}
