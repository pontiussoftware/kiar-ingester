import {AbstractControl, ValidationErrors, ValidatorFn} from "@angular/forms";

/** A custom {@link ValidatorFn} that makes sure, that two passwords are the same. */
export const PASSWORD_VALIDATOR: ValidatorFn = (group: AbstractControl):  ValidationErrors | null => {
  let passwordFirst = group.get('passwordFirst')?.value
  let passwordSecond = group.get('passwordSecond')?.value
  if (passwordFirst == null || passwordSecond == null) return null
  if (passwordFirst.length < PASSWORD_MIN_LENGTH) return { notLongEnough: true }
  return passwordFirst === passwordSecond ? null : { notSame: true }
}

/** The minimum length of a password (for validation). */
export const PASSWORD_MIN_LENGTH: number = 8
