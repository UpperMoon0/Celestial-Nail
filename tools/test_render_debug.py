import unittest
from check_render_debug import check_log


def sample(check="PASS", result="PIXELS_CHANGED", pixels=8, pack="true", force="false"):
    return (f"[NailRenderDebug] check={check} result={result} changedPixels={pixels} "
            f"forceVisible={force} disableDistanceFade=false oculus=packInUse={pack} shadowPass=false")


class LiveRenderRegressionTest(unittest.TestCase):
    def test_requires_repeated_actual_shader_draw_pixels(self):
        self.assertEqual(2, check_log(sample() + "\n" + sample()))
        for text in ("", sample(), sample(pack="false") + "\n" + sample(),
                     sample(force="true") + "\n" + sample(), sample(pixels=0) + "\n" + sample()):
            with self.subTest(text=text), self.assertRaises(ValueError):
                check_log(text)

    def test_invisible_draw_fails_even_if_a_later_interval_passes(self):
        for result in ("NO_RENDER", "NO_VERTICES", "TRANSPARENT", "NO_SHADER_APPLY",
                       "WRONG_PROGRAM", "NO_PIXEL_CHANGE", "DRAW_OBSERVED"):
            with self.subTest(result=result), self.assertRaises(ValueError):
                check_log(sample("FAIL", result) + "\n" + sample() + "\n" + sample())

    def test_menu_and_pre_emergence_do_not_count_as_successful_samples(self):
        unarmed = sample("NOT_ARMED", "NO_RENDER", 0)
        self.assertEqual(2, check_log(unarmed + "\n" + sample() + "\n" + sample()))
        with self.assertRaises(ValueError):
            check_log(unarmed)


if __name__ == "__main__":
    unittest.main()
