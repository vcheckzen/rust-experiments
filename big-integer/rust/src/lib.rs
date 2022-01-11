use std::cmp::Ordering;
use std::fmt::{Display, Formatter};
use std::ops;

#[derive(Clone, PartialEq)]
pub struct BigInt {
    positive: bool,
    value: Vec<i8>,
}

impl Display for BigInt {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        let sign = match self.positive {
            true => "",
            false => "-"
        };
        write!(f, "{}{}", sign,
               self.value
                   .iter()
                   .fold(String::new(), |acc, &p| acc + &p.to_string()))
    }
}

impl PartialOrd for BigInt {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        let ordering = self.positive.cmp(&other.positive);
        if ordering != Ordering::Equal {
            return Some(ordering);
        }

        let ordering = self.value.len().cmp(&other.value.len());
        if ordering != Ordering::Equal {
            return Some(ordering);
        }

        Some(self.value.cmp(&other.value))
    }
}

impl BigInt {
    pub fn new(v: &str) -> Self {
        if v.len() == 0 { panic!("IllegalArgument") }

        let mut positive = true;
        let mut begin_index = 0;
        match v.chars().nth(0).unwrap() {
            '-' => {
                positive = false;
                begin_index += 1;
            }
            '+' => begin_index += 1,
            _ => {}
        };
        while begin_index < v.len() && v.chars().nth(begin_index).unwrap() == '0' {
            begin_index += 1;
        }
        if begin_index == v.len() { begin_index -= 1; }

        let mut integer = Self {
            positive,
            value: Vec::with_capacity(v.len() - begin_index),
        };
        for i in begin_index..v.len() {
            let c = v.chars().nth(i).unwrap();
            if c < '0' || c > '9' { panic!("IllegalArgument") }
            integer.value.push(c.to_digit(10).unwrap() as i8);
        }

        integer.set_zero_positive();
        integer
    }

    fn set_zero_positive(&mut self) {
        if self.value.len() == 1 && self.value[0] == 0 {
            self.positive = true;
        }
    }

    fn abs(self) -> Self {
        Self {
            positive: true,
            value: self.value.clone(),
        }
    }

    fn trim_zero(&mut self) {
        let mut i = 0usize;
        for v in self.value.iter() {
            if v != &0 { break; }
            i += 1;
        }
        self.value.drain(0..i);
    }
}

impl ops::Neg for BigInt {
    type Output = Self;

    fn neg(self) -> Self::Output {
        if self.value == vec![0] {
            return self.clone();
        }

        Self {
            positive: !self.positive,
            value: self.value.clone(),
        }
    }
}

impl ops::Add<BigInt> for BigInt {
    type Output = Self;

    fn add(self, rhs: Self) -> Self::Output {
        if self.positive != rhs.positive {
            return if self.positive {
                self - (-rhs)
            } else {
                rhs - (-self)
            };
        }

        // 以下同号
        let (mut longer, mut shorter) = (&self, &rhs);
        if self.value.len() < rhs.value.len() {
            longer = &rhs;
            shorter = &self;
        }
        let mut sum = Self {
            positive: self.positive,
            value: vec![0; longer.value.len() + 1],
        };

        let mut end = longer.value.len();
        for (i, j) in (0..end).rev()
            .zip((0..shorter.value.len()).rev()) {
            let k = i + 1;
            let s = longer.value[i] + shorter.value[j] + sum.value[k];
            sum.value[k] = s % 10;
            sum.value[i] = s / 10;
            end = i;
        }
        for i in (0..end).rev() {
            let k = i + 1;
            let s = longer.value[i] + sum.value[k];
            sum.value[k] = s % 10;
            sum.value[i] = s / 10;
        }

        sum.trim_zero();
        sum
    }
}

impl ops::Sub<BigInt> for BigInt {
    type Output = Self;

    fn sub(self, rhs: Self) -> Self::Output {
        if self == rhs {
            return Self::new("0");
        }

        if self.positive != rhs.positive {
            return if self.positive {
                self + -rhs
            } else {
                -(rhs + -self)
            };
        }

        if self.value.len() < rhs.value.len() ||
            (self.value.len() == rhs.value.len()
                && self.value < rhs.value) {
            return Self {
                positive: !self.positive,
                value: (rhs.abs() - self.abs()).value,
            };
        } else if !self.positive {
            return Self {
                positive: false,
                value: (self.abs() - rhs.abs()).value,
            };
        };

        if self.value.len() == 1 {
            return Self {
                positive: true,
                value: vec![self.value[0] - rhs.value[0]],
            };
        }

        // 以下 self > rhs > 0
        let mut diff = Self {
            positive: true,
            value: vec![9; self.value.len() + 1],
        };
        diff.value[0] = 0;
        diff.value[1] = -1;
        diff.value[self.value.len()] = 10;

        let mut end = self.value.len();
        for (i, j) in (0..end).rev()
            .zip((0..rhs.value.len()).rev()) {
            let k = i + 1;
            let d = self.value[i] + diff.value[k] - rhs.value[j];
            diff.value[k] = d % 10;
            diff.value[i] += d / 10;
            end = i;
        }
        for i in (0..end).rev() {
            let k = i + 1;
            let d = self.value[i] + diff.value[k];
            diff.value[k] = d % 10;
            diff.value[i] += d / 10;
        }

        diff.trim_zero();
        diff
    }
}

impl ops::Mul<BigInt> for BigInt {
    type Output = Self;

    fn mul(self, rhs: Self) -> Self::Output {
        let zero = Self::new("0");
        if self == zero || rhs == zero {
            return zero;
        }

        let one = vec![1];
        let sign = self.positive == rhs.positive;
        let mut one_mul: Option<Self> = None;
        if self.value == one {
            one_mul = Some(rhs.clone());
        } else if rhs.value == one {
            one_mul = Some(self.clone());
        }
        if let Some(mut x) = one_mul {
            x.positive = sign;
            return x;
        }

        let mut product = Self {
            positive: sign,
            value: vec![0; self.value.len() + rhs.value.len()],
        };

        for i in (0..self.value.len()).rev() {
            for j in (0..rhs.value.len()).rev() {
                let h = i + j;
                let l = h + 1;
                let p = self.value[i] * rhs.value[j] + product.value[l];

                product.value[l] = p % 10;
                product.value[h] += p / 10;
            }
        }

        product.trim_zero();
        product
    }
}

impl ops::Div<BigInt> for BigInt {
    type Output = Self;

    fn div(self, rhs: Self) -> Self::Output {
        let zero = Self::new("0");
        if rhs == zero { panic!("divisor can't be 0") }
        let one = vec![1];
        let sign = self.positive == rhs.positive;
        if rhs.value == one {
            return Self {
                positive: sign,
                value: self.value.clone(),
            };
        }
        if self.value.len() < rhs.value.len() ||
            (self.value.len() == rhs.value.len()
                && self.value < rhs.value) {
            return zero;
        }
        if self.value == rhs.value {
            return Self {
                positive: sign,
                value: one,
            };
        }
        if !sign || !self.positive {
            return Self {
                positive: sign,
                value: (self.abs() / rhs.abs()).value,
            };
        }

        // 以下 self > rhs > 0
        let mut quotient = Self {
            positive: sign,
            value: vec![],
        };

        let mut dividend = self.clone();
        'outer: loop {
            let mut diff = Self {
                positive: true,
                value: vec![],
            };

            // self > rhs ensures existence of diff which > rhs
            let mut i = 0usize;
            loop {
                diff.value.push(dividend.value[i]);
                i += 1;
                if diff >= rhs { break; }
            }

            // diff >= rhs first turns true, ensures 1 <= c <= 9
            let mut c = 1;
            loop {
                diff = diff - rhs.clone();
                if diff < rhs { break; }
                c += 1;
            }
            quotient.value.push(c);

            if i >= dividend.value.len() { break; }

            let mut rest = Self {
                positive: true,
                value: vec![],
            };
            if diff == zero {
                // append zeros
                while dividend.value[i] == 0 {
                    quotient.value.push(0);
                    i += 1;
                    if i >= dividend.value.len() {
                        break 'outer;
                    }
                }
            } else {
                rest.value.extend(&diff.value)
            }

            // append zeros
            loop {
                rest.value.push(dividend.value[i]);
                i += 1;
                if rest < rhs {
                    quotient.value.push(0);
                } else {
                    break;
                }
                if i >= dividend.value.len() {
                    break 'outer;
                }
            }

            rest.value.extend(&dividend.value[i..]);

            if rest < rhs { break; }

            dividend = rest;
        }

        quotient
    }
}

#[cfg(test)]
mod tests {
    use std::panic::catch_unwind;

    use num_bigint::{RandBigInt, ToBigInt};

    use super::*;

    #[test]
    fn test_integer_format() {
        assert!(catch_unwind(|| BigInt::new("a")).is_err());
        assert!(catch_unwind(|| BigInt::new("1234+")).is_err());
        assert!(catch_unwind(|| BigInt::new("--1234")).is_err());
        assert!(catch_unwind(|| BigInt::new("++1234")).is_err());
        assert!(catch_unwind(|| BigInt::new("+12.34")).is_err());

        assert_eq!(format!("{}", BigInt::new("1234")), "1234".to_string());
        assert_eq!(format!("{}", BigInt::new("+1234")), "1234".to_string());
        assert_eq!(format!("{}", BigInt::new("-1234")), "-1234".to_string());
        assert_eq!(format!("{}", BigInt::new("001234")), "1234".to_string());
        assert_eq!(format!("{}", BigInt::new("+001234")), "1234".to_string());
        assert_eq!(format!("{}", BigInt::new("-001234")), "-1234".to_string());
    }

    #[test]
    fn test_comparator() {
        assert!(BigInt::new("0") == BigInt::new("0"));
        assert!(BigInt::new("0") != BigInt::new("1"));
        assert!(BigInt::new("1") != -BigInt::new("1"));
        assert!(BigInt::new("1") < BigInt::new("2"));
        assert!(BigInt::new("1") <= BigInt::new("1"));
        assert!(BigInt::new("2") > BigInt::new("1"));
        assert!(BigInt::new("2") >= BigInt::new("2"));
        assert!(BigInt::new("10") > BigInt::new("9"));
    }

    #[test]
    fn test_ng_operator() {
        assert!(BigInt::new("100") != -BigInt::new("100"));
        assert_eq!(format!("{}", -BigInt::new("100")), "-100".to_string());
    }


    fn test_operator(f: fn(BigInt, BigInt, num_bigint::BigInt, num_bigint::BigInt)
                           -> (BigInt, num_bigint::BigInt)) {
        let mut rng = rand::thread_rng();
        let low = -10000.to_bigint().unwrap();
        let high = 10000.to_bigint().unwrap();

        for _ in 0..1000 {
            let a = rng.gen_bigint(1000);
            let b = rng.gen_bigint_range(&low, &high);
            let a_string = format!("{}", a);
            let b_string = format!("{}", b);

            let (ret, expected_ret) = f(
                BigInt::new(a_string.as_str()),
                BigInt::new(b_string.as_str()),
                a,
                b,
            );
            assert_eq!(format!("{}", ret), format!("{}", expected_ret));
        }
    }

    #[test]
    fn test_add_operator() {
        test_operator(|tested_a, tested_b, a, b|
            (tested_a + tested_b, a + b)
        );
    }

    #[test]
    fn test_sub_operator() {
        test_operator(|tested_a, tested_b, a, b|
            (tested_a - tested_b, a - b)
        );
    }

    #[test]
    fn test_mul_operator() {
        test_operator(|tested_a, tested_b, a, b|
            (tested_a * tested_b, a * b)
        );
    }

    #[test]
    #[should_panic]
    fn test_div_zero() {
        let _ = BigInt::new("100") / BigInt::new("0");
    }

    #[test]
    fn test_div_operator() {
        test_operator(|tested_a, tested_b, a, b|
            (tested_a / tested_b, a / b)
        );
    }
}
